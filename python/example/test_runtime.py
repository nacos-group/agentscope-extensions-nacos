import asyncio
from contextlib import asynccontextmanager

from agentscope.agent import ReActAgent
from agentscope.model import DashScopeChatModel
from agentscope_runtime.engine import Runner, LocalDeployManager
from agentscope_runtime.engine.agents.agentscope_agent import AgentScopeAgent
from agentscope_runtime.engine.services.context_manager import ContextManager
from v2.nacos import ClientConfigBuilder

from agentscope_nacos.a2a.nacos_a2a_adapter import A2AFastAPINacosAdaptor

from agentscope_nacos.nacos_react_agent import NacosReActAgent, NacosAgentListener

client_config = (ClientConfigBuilder()
				 .server_address("localhost:8848")
				 .namespace_id("public")
				 .log_level('DEBUG')  # 设置为 DEBUG 级别
				 .build())
nacos_agent_listener = NacosAgentListener(nacos_client_config=client_config,
											agent_name="test")


agent: AgentScopeAgent | None = None

print("✅ AgentScope agent created successfully")


@asynccontextmanager
async def create_runner():
	global agent
	await nacos_agent_listener.initialize()
	agent = AgentScopeAgent(
			name="Friday",
			model=nacos_agent_listener.chat_model,
			agent_config={
				"nacos_agent_listener": nacos_agent_listener,
			},
			agent_builder=NacosReActAgent,
	)

	async with Runner(
			agent=agent,
			context_manager=ContextManager(),
	) as runner:
		print("✅ Runner创建成功")
		yield runner



async def deploy_agent(runner):
	# 创建部署管理器
	deploy_manager = LocalDeployManager(
			host="localhost",
			port=8090,
	)

	a2a_protocol = A2AFastAPINacosAdaptor(
			nacos_client_config=client_config,
			agent=agent,
			host="localhost",
	)
	# 将智能体部署为流式服务
	deploy_result = await runner.deploy(
			deploy_manager=deploy_manager,
			endpoint_path="/process",
			protocol_adapters=[a2a_protocol],
			stream=True,  # Enable streaming responses
	)
	print(f"🚀智能体部署在: {deploy_result}")
	print(f"🌐服务URL: {deploy_manager.service_url}")
	print(f"💚 健康检查: {deploy_manager.service_url}/health")

	return deploy_manager


async def run_deployment():
	async with create_runner() as runner:
		deploy_manager = await deploy_agent(runner)

	# Keep the service running (in production, you'd handle this differently)
	print("🏃 Service is running...")
	await asyncio.sleep(1000)

	return deploy_manager


if __name__ == "__main__":
	asyncio.run(run_deployment())