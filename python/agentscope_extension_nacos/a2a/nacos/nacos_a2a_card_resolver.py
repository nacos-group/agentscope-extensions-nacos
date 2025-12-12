import logging
from typing import Any

from a2a.types import AgentCard

from agentscope_extension_nacos.a2a.a2a_card_resolver import \
	AgentCardResolverBase

# Initialize logger
logger = logging.getLogger(__name__)


class NacosAgentCardResolver(AgentCardResolverBase):
	"""Nacos-based A2A Agent Card resolver.

	Resolves and subscribes to agent cards stored in Nacos A2A registry.
	Supports automatic updates when agent cards change in Nacos.
	"""

	def __init__(
			self,
			remote_agent_name: str,
			nacos_client_config: Any | None = None,
			version: str | None = None,
	) -> None:
		"""Initialize the NacosAgentCardResolver.

		Args:
			remote_agent_name (`str`):
				Name of the remote agent in Nacos.
			nacos_client_config (`Any | None`, optional):
				Nacos client configuration. If None, uses default config.
				Defaults to None.
			version (`str | None`, optional):
				Version constraint for the agent card.
				Defaults to None.

		Raises:
			ValueError: If remote_agent_name is empty.
		"""
		if not remote_agent_name:
			raise ValueError("remote_agent_name is required")

		self._nacos_client_config = nacos_client_config
		self._remote_agent_name = remote_agent_name
		self._version = version

		# Lazy initialization state
		self._initialized = False
		self._nacos_ai_service: Any | None = None
		self._agent_card: AgentCard | None = None

	async def get_agent_card(self) -> AgentCard:
		"""Get agent card from Nacos with lazy initialization.

		Returns:
			`AgentCard`:
				The resolved agent card from Nacos.

		Raises:
			RuntimeError:
				If failed to fetch agent card from Nacos.
		"""
		await self._ensure_initialized()

		if self._agent_card is None:
			raise RuntimeError(
					f"Failed to get agent card for {self._remote_agent_name}",
			)

		return self._agent_card

	async def _ensure_initialized(self) -> None:
		"""Ensure the resolver is initialized.

		Performs lazy initialization on first call, including:
		- Creating NacosAIService
		- Fetching agent card from Nacos
		- Subscribing to agent card updates
		"""
		if self._initialized:
			return

		# Lazy import third-party libraries
		from v2.nacos.ai.model.ai_param import (
			GetAgentCardParam,
			SubscribeAgentCardParam,
		)
		from v2.nacos.ai.nacos_ai_service import NacosAIService

		try:
			logger.debug(
					"[%s] Initializing for agent: %s",
					self.__class__.__name__,
					self._remote_agent_name,
			)

			# Create Nacos AI service
			self._nacos_ai_service = await NacosAIService.create_ai_service(
					self._nacos_client_config,
			)

			# Fetch agent card from Nacos
			self._agent_card = await self._nacos_ai_service.get_agent_card(
					GetAgentCardParam(
							agent_name=self._remote_agent_name,
							version=self._version,
					),
			)

			logger.debug(
					"[%s] Agent card fetched from Nacos: %s",
					self.__class__.__name__,
					self._agent_card.name if self._agent_card else "None",
			)

			# Subscribe to agent card updates
			async def agent_card_subscriber(
					agent_name: str,
					agent_card: AgentCard,
			) -> None:
				"""Callback for agent card updates from Nacos."""
				logger.debug(
						"[%s] Agent card updated for %s: %s",
						self.__class__.__name__,
						agent_name,
						agent_card.name,
				)
				self._agent_card = agent_card

			await self._nacos_ai_service.subscribe_agent_card(
					SubscribeAgentCardParam(
							agent_name=self._remote_agent_name,
							version=self._version,
							subscribe_callback=agent_card_subscriber,
					),
			)

			logger.debug(
					"[%s] Subscribed to agent card updates for: %s",
					self.__class__.__name__,
					self._remote_agent_name,
			)

			self._initialized = True

		except Exception as e:
			logger.error(
					"[%s] Failed to initialize Nacos resolver: %s",
					self.__class__.__name__,
					e,
			)
			raise RuntimeError(
					f"Failed to initialize Nacos resolver for "
					f"{self._remote_agent_name}: {e}",
			) from e
