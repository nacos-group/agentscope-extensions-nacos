import asyncio
import os

from agentscope_nacos.nacos_react_agent import NacosAgentListener, \
	NacosReActAgent
from agentscope_nacos.nacos_service_manager import NacosServiceManager
from agentscope.agent import ReActAgent, UserAgent, UserInputBase, UserInputData
from agentscope.formatter import DashScopeChatFormatter
from agentscope.memory import InMemoryMemory
from agentscope.message import Msg, TextBlock
from agentscope.model import DashScopeChatModel
from v2.nacos import ClientConfigBuilder




async def creating_react_agent() -> None:


	client_config = (ClientConfigBuilder()
					 .server_address("localhost:8848")
					 .namespace_id("public")
					 .log_level('DEBUG')  # 设置为 DEBUG 级别
					 .build())

	#配置全局 Nacos 连接参数
	NacosServiceManager.set_global_config(client_config)

	nacos_agent_listener = NacosAgentListener(agent_name="test")

	await nacos_agent_listener.initialize()

	jarvis = NacosReActAgent(
			nacos_agent_listener= nacos_agent_listener,
			name="Jarvis",
	)


	# """ 或者托管一个已有的智能体 """
	#
	# jarvis = ReActAgent(
	# 		name="Jarvis",
	# 		sys_prompt="你是一个AI助手",
	# 		model=DashScopeChatModel(
	# 				model_name="qwen-max",
	# 				api_key=os.getenv("DASH_SCOPE_API_KEY"),
	# 		),
	# 		formatter=DashScopeChatFormatter(),
	# 		memory=InMemoryMemory(),
	# )
	#
	#
	# nacos_agent_listener.attach_agent(jarvis)





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

