import asyncio
import os

from agentscope_nacos.model.nacos_chat_model import NacosChatModel
from agentscope_nacos.nacos_service_manager import NacosServiceManager

from agentscope.agent import ReActAgent, UserAgent, UserInputBase, UserInputData
from agentscope.formatter import DashScopeChatFormatter
from agentscope.memory import InMemoryMemory
from agentscope.message import TextBlock
from v2.nacos import ClientConfigBuilder

from agentscope_nacos.mcp.agentscope_nacos_mcp import NacosHttpStatelessClient, \
	NacosHttpStatefulClient
# 使用 DynamicToolkit 替代 Toolkit，支持工具动态更新
from agentscope_nacos.mcp.agentscope_dynamic_toolkit import DynamicToolkit


client_config = (ClientConfigBuilder()
					 .server_address("localhost:8848")
					 .namespace_id("public")
					 .log_level('DEBUG')  # 设置为 DEBUG 级别
					 .build())
#配置全局 Nacos 连接参数
NacosServiceManager.set_global_config(client_config)

async def creating_react_agent() -> None:
	"""创建一个 ReAct 智能体并运行一个简单任务。"""

	# 使用 Nacos 中的 MCP Server
	stateless_client = NacosHttpStatelessClient("nacos-mcp-1")
	stateful_client = NacosHttpStatefulClient("nacos-mcp-2")

	toolkit = DynamicToolkit()
	await stateful_client.connect()
	await toolkit.register_mcp_client(stateful_client)
	await toolkit.register_mcp_client(stateless_client)

	#使用 Nacos 中的模型配置
	model = NacosChatModel(
			agent_name="test-agent",
			stream=True,
	)

	# Build Agent
	jarvis = ReActAgent(
			name="Jarvis",
			sys_prompt="你是一个AI助手",
			model = model,
			formatter=DashScopeChatFormatter(),
			toolkit=toolkit,
			memory=InMemoryMemory(),
	)



	class ThreadedTerminalInput(UserInputBase):
		"""在线程池中运行 input()，避免阻塞事件循环"""

		def __init__(self, input_hint: str = "User Input: ") -> None:
			self.input_hint = input_hint

		async def __call__(self, agent_id: str, agent_name: str,
				structured_model=None, *args, **kwargs):
			loop = asyncio.get_event_loop()
			text_input = await loop.run_in_executor(None, input,
													self.input_hint)
			return UserInputData(
					blocks_input=[TextBlock(type="text", text=text_input)],
					structured_input=None
			)

	# 在创建 UserAgent 后：
	user = UserAgent(name="user")
	user.override_instance_input_method(ThreadedTerminalInput())

	msg = None
	msg = await user(msg)

	while True:
		msg = await jarvis(msg)
		msg = await user(msg)
		if msg.get_text_content() == "exit":
			break





if __name__ == "__main__":
	asyncio.run(creating_react_agent())

