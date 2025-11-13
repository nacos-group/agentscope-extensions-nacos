import asyncio
import logging
from typing import Callable, Optional

from a2a.server.apps import A2AFastAPIApplication
from a2a.server.request_handlers import DefaultRequestHandler
from a2a.server.tasks import InMemoryTaskStore
from a2a.types import AgentCard, AgentCapabilities, AgentSkill
from agentscope_runtime.engine.agents import Agent
from agentscope_runtime.engine.deployers.adapter.a2a import \
	A2AFastAPIDefaultAdapter
from agentscope_runtime.engine.deployers.adapter.a2a.a2a_agent_adapter import \
	A2AExecutor
from agentscope_runtime.engine.deployers.adapter.protocol_adapter import \
	ProtocolAdapter
from v2.nacos import ClientConfig
from v2.nacos.ai.model.ai_param import ReleaseAgentCardParam, \
	RegisterAgentEndpointParam
from v2.nacos.ai.nacos_ai_service import NacosAIService

from agentscope_nacos.utils import get_first_non_loopback_ip
from agentscope_nacos.nacos_service_manager import NacosServiceManager


# Initialize logger
logger = logging.getLogger(__name__)


class A2AFastAPINacosAdaptor(ProtocolAdapter):
	"""FastAPI-based A2A protocol adapter with Nacos service registration.
	
	This adapter exposes an AgentScope agent as an A2A protocol-compliant
	service and automatically registers it to Nacos for service discovery.
	Uses NacosServiceManager for connection pooling.
	
	Features:
		- Automatic A2A protocol endpoint creation
		- Agent card generation and publication to Nacos
		- Service endpoint registration in Nacos
		- Background registration with error handling
		
	Args:
		agent: The AgentScope agent to expose
		nacos_client_config: Optional Nacos client config (uses global if None)
		host: Server host (auto-detected if None)
		port: Server port (default: 8090)
		**kwargs: Additional arguments for ProtocolAdapter
	"""

	def __init__(
		self,
		agent,
		nacos_client_config: Optional[ClientConfig] = None,
		host: str | None = None,
		port: int = 8090,
		**kwargs
	):
		super().__init__(**kwargs)
		self._agent = agent
		self._host = host or get_first_non_loopback_ip()
		self._port = port
		self._nacos_client_config = nacos_client_config
		self.nacos_ai_service: NacosAIService | None = None
		self._root_path: str = ""
		self._agent_card: AgentCard | None = None
		self._register_task: asyncio.Task | None = None
		
		logger.info(f"[{self.__class__.__name__}] Initialized for agent: {agent.name} at {self._host}:{self._port}")

	def add_endpoint(self, app, func: Callable, **kwargs):
		"""Add A2A protocol endpoints to FastAPI application.
		
		Creates agent card, sets up request handlers, and initiates
		background Nacos registration.
		
		Args:
			app: FastAPI application instance
			func: Agent's stream_query function
			**kwargs: Additional arguments
		"""
		# Extract root_path from app
		app_root_path = getattr(app, 'root_path', '')
		if app_root_path:
			# root_path usually starts with /, use it directly
			self._root_path = app_root_path
			logger.debug(f"[{self.__class__.__name__}] Using root_path: {self._root_path}")
		
		# Create request handler with agent executor
		request_handler = DefaultRequestHandler(
				agent_executor=A2AExecutor(func=func),
				task_store=InMemoryTaskStore(),
		)

		# Create agent card with correct URL
		self._agent_card = self._create_agent_card()
		logger.info(f"[{self.__class__.__name__}] Agent card created for: {self._agent_card.name}")
		logger.debug(f"[{self.__class__.__name__}] Agent card:\n{self._agent_card.model_dump_json(indent=2)}")

		# Create A2A FastAPI application
		server = A2AFastAPIApplication(
				agent_card=self._agent_card,
				http_handler=request_handler,
		)

		# Add routes to the app
		server.add_routes_to_app(app)
		logger.info(f"[{self.__class__.__name__}] A2A routes added to FastAPI application")
		
		# Start background Nacos registration task
		self._start_register_task()

	def _start_register_task(self):
		"""Start background Nacos registration task (safe).
		
		Creates an asyncio task for Nacos registration if event loop is available.
		Safely handles cases where no event loop is running (e.g., testing).
		"""
		try:
			loop = asyncio.get_running_loop()
			self._register_task = loop.create_task(self._register_to_nacos())
			logger.info(f"[{self.__class__.__name__}] Nacos registration task started in background")
		except RuntimeError:
			# No running event loop, might be in test environment
			logger.warning(f"[{self.__class__.__name__}] No running event loop, skipping Nacos registration")
	
	def _create_agent_card(self) -> AgentCard:
		"""Create agent card with complete URL information.
		
		Builds an A2A protocol-compliant agent card containing:
		- Agent capabilities
		- Available skills
		- Endpoint URL (http://{host}:{port}{root_path})
		- Agent metadata
		
		Returns:
			AgentCard: Complete agent card object
		"""
		# Define agent capabilities
		capabilities = AgentCapabilities(
				streaming=False,
				push_notifications=False,
		)
		
		# Define agent skills
		skill = AgentSkill(
				id="dialog",
				name="Natural Language Dialog Skill",
				description="Enables natural language conversation and dialogue "
							"with users",
				tags=["natural language", "dialog", "conversation"],
				examples=[
					"Hello, how are you?",
					"Can you help me with something?",
				],
		)

		# Build complete URL: http://{host}:{port}{root_path}
		# root_path already contains leading /, so concatenate directly
		url = f"http://{self._host}:{self._port}{self._root_path}"
		logger.debug(f"[{self.__class__.__name__}] Agent card URL: {url}")

		return AgentCard(
				capabilities=capabilities,
				skills=[skill],
				name=self._agent.name,
				description=self._agent.description,
				default_input_modes=["text"],
				default_output_modes=["text"],
				url=url,
				version="1.0.0",
		)


	async def _register_to_nacos(self):
		"""Register agent to Nacos (background task).
		
		Performs two-step registration:
		1. Release agent card to Nacos
		2. Register agent endpoint with host/port information
		
		Uses NacosServiceManager for connection pooling.
		
		Raises:
			Exception: If registration fails
		"""
		try:
			logger.info(f"[{self.__class__.__name__}] Starting Nacos registration for agent: {self._agent_card.name}")
			
			# Get Nacos AI service with connection pooling
			manager = NacosServiceManager()
			self.nacos_ai_service = await manager.get_ai_service(
				self._nacos_client_config
			)
			
			# Step 1: Publish agent card to Nacos
			await self.nacos_ai_service.release_agent_card(
				ReleaseAgentCardParam(
					agent_card=self._agent_card
				)
			)
			logger.info(f"[{self.__class__.__name__}] Agent card published to Nacos: {self._agent_card.name}")
			
			# Step 2: Register agent endpoint
			await self.nacos_ai_service.register_agent_endpoint(
				RegisterAgentEndpointParam(
					agent_name=self._agent_card.name,
					version=self._agent_card.version,
					address=self._host,
					port=self._port,
					path=self._root_path,
				)
			)
			logger.info(f"[{self.__class__.__name__}] Agent endpoint registered: {self._host}:{self._port}{self._root_path}")
			logger.info(f"[{self.__class__.__name__}] ✅ Agent '{self._agent_card.name}' successfully registered to Nacos")
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] ❌ Nacos registration failed: {e}")
			raise
	
	async def wait_for_registration(self):
		"""Wait for Nacos registration to complete (optional).
		
		Can be called to ensure registration is finished before proceeding.
		"""
		if self._register_task:
			logger.debug(f"[{self.__class__.__name__}] Waiting for Nacos registration to complete...")
			await self._register_task
			logger.debug(f"[{self.__class__.__name__}] Nacos registration completed")



