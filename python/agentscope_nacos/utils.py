import re
import random
import logging
from contextlib import asynccontextmanager
import socket

import psutil
from v2.nacos.ai.model.mcp.mcp import McpEndpointInfo, McpServerDetailInfo

# Initialize logger
logger = logging.getLogger(__name__)

def get_first_non_loopback_ip():
	"""Get the first non-loopback IPv4 address from network interfaces.
	
	Returns:
		str | None: The first non-loopback IP address, or None if not found
	"""
	for interface, addrs in psutil.net_if_addrs().items():
		for addr in addrs:
			if addr.family == socket.AF_INET and not addr.address.startswith(
					'127.'):
				logger.debug(f"Found non-loopback IP: {addr.address} on interface {interface}")
				return addr.address
	logger.warning("No non-loopback IP address found")
	return None

def generate_url_from_endpoint(endpoint: McpEndpointInfo):
	"""Generate URL from MCP endpoint information.
	
	Args:
		endpoint: McpEndpointInfo object containing endpoint details
		
	Returns:
		str: Generated URL string
	"""
	protocol = endpoint.protocol
	if endpoint.protocol is None or len(endpoint.protocol) == 0:
		if endpoint.port == 443:
			protocol = "https"
		else:
			protocol = "http"
	url = f"{protocol}://{endpoint.address}:{endpoint.port}{endpoint.path}"
	logger.debug(f"Generated URL from endpoint: {url}")
	return url


def random_generate_url_from_mcp_server_detail_info(mcp_server_detail_info: McpServerDetailInfo):
	"""Randomly select an endpoint from MCP server details and generate URL.
	
	Args:
		mcp_server_detail_info: McpServerDetailInfo object containing backend endpoints
		
	Returns:
		str: Generated URL from randomly selected endpoint
	"""
	selected_endpoint = random.choice(
			mcp_server_detail_info.backendEndpoints)
	url = generate_url_from_endpoint(selected_endpoint)
	logger.debug(f"Randomly selected endpoint URL: {url}")
	return url

def validate_agent_name(agent_name: str) -> str:
	"""Validate and process agent name.
	
	Ensures agent name conforms to naming standards:
	- Cannot be empty
	- Max length 128 characters
	- Only alphanumeric characters and '.', ':', '_', '-'
	- Spaces are replaced with underscores

	Args:
		agent_name: Original agent name

	Returns:
		str: Processed agent name

	Raises:
		ValueError: When agent name does not conform to standards
	"""
	if not agent_name:
		logger.error("Agent name validation failed: empty name")
		raise ValueError("Agent name cannot be empty")

	# Replace spaces with underscores
	original_name = agent_name
	agent_name = agent_name.replace(' ', '_')
	if original_name != agent_name:
		logger.debug(f"Agent name spaces replaced: '{original_name}' -> '{agent_name}'")

	# Check length
	if len(agent_name) > 128:
		logger.error(f"Agent name validation failed: name too long ({len(agent_name)} chars): {agent_name}")
		raise ValueError("Agent name cannot exceed 128 characters")

	# Check if characters conform to standards: letters, digits, '.', ':', '_', '-'
	if not re.match(r'^[a-zA-Z0-9._:-]+$', agent_name):
		logger.error(f"Agent name validation failed: invalid characters in '{agent_name}'")
		raise ValueError("Agent name can only contain letters, digits, '.', ':', '_', and '-'")

	logger.debug(f"Agent name validated successfully: {agent_name}")
	return agent_name


import asyncio


class AsyncRWLock:
	"""Async Read-Write Lock.
	
	Provides read-write lock semantics for async code:
	- Multiple readers can acquire the lock simultaneously
	- Only one writer can acquire the lock at a time
	- Writers have exclusive access (no readers or writers)
	
	Example:
		```python
		rwlock = AsyncRWLock()
		
		# Read access
		async with rwlock.read_lock():
		    data = shared_data
		
		# Write access
		async with rwlock.write_lock():
		    shared_data = new_value
		```
	"""

	def __init__(self):
		self._readers = 0
		self._writers = 0
		self._lock = asyncio.Lock()
		self._reader_condition = asyncio.Condition(self._lock)
		self._writer_condition = asyncio.Condition(self._lock)
		logger.debug(f"[{self.__class__.__name__}] Initialized")

	async def acquire_read(self):
		"""Acquire read lock (async).
		
		Waits if any writer holds the lock.
		Multiple readers can hold the lock simultaneously.
		"""
		async with self._lock:
			while self._writers > 0:
				await self._reader_condition.wait()
			self._readers += 1
			logger.debug(f"[{self.__class__.__name__}] Read lock acquired (readers: {self._readers})")

	async def release_read(self):
		"""Release read lock (async).
		
		Notifies waiting writers when last reader releases.
		"""
		async with self._lock:
			self._readers -= 1
			logger.debug(f"[{self.__class__.__name__}] Read lock released (readers: {self._readers})")
			if self._readers == 0:
				self._writer_condition.notify_all()

	async def acquire_write(self):
		"""Acquire write lock (async).
		
		Waits until no readers or writers hold the lock.
		Only one writer can hold the lock at a time.
		"""
		async with self._lock:
			while self._readers > 0 or self._writers > 0:
				await self._writer_condition.wait()
			self._writers += 1
			logger.debug(f"[{self.__class__.__name__}] Write lock acquired")

	async def release_write(self):
		"""Release write lock (async).
		
		Notifies all waiting readers and writers.
		"""
		async with self._lock:
			self._writers -= 1
			logger.debug(f"[{self.__class__.__name__}] Write lock released")
			self._reader_condition.notify_all()
			self._writer_condition.notify_all()

	@asynccontextmanager
	async def read_lock(self):
		"""Read lock context manager.
		
		Usage:
			async with rwlock.read_lock():
			    # Read shared data
			    data = shared_data
		"""
		await self.acquire_read()
		try:
			yield
		finally:
			await self.release_read()

	@asynccontextmanager
	async def write_lock(self):
		"""Write lock context manager.
		
		Usage:
			async with rwlock.write_lock():
			    # Modify shared data
			    shared_data = new_value
		"""
		await self.acquire_write()
		try:
			yield
		finally:
			await self.release_write()