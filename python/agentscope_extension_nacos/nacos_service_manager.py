# -*- coding: utf-8 -*-
"""
Nacos Service Manager - Global Singleton Pattern

Provides unified Nacos service management to avoid duplicate connections.
Same ClientConfig reuses the same set of service instances (NacosNamingService, NacosConfigService, NacosAIService).

Usage:
    # Method 1: Use global config (from environment variables)
    manager = NacosServiceManager()
    ai_service = await manager.get_ai_service()
    
    # Method 2: Use custom config
    ai_service = await manager.get_ai_service(client_config=my_config)
    
    # Cleanup resources
    await NacosServiceManager.cleanup()

Environment Variable Configuration:
    NACOS_SERVER_ADDRESS=localhost:8848           # Required
    NACOS_NAMESPACE_ID=public                     # Required
    NACOS_ACCESS_KEY=xxx                          # Optional (Alibaba Cloud MSE)
    NACOS_SECRET_KEY=yyy                          # Optional (Alibaba Cloud MSE)
    NACOS_USERNAME=nacos                          # Optional (Local Nacos)
    NACOS_PASSWORD=nacos                          # Optional (Local Nacos)
    NACOS_LOG_LEVEL=INFO                          # Optional
"""
import asyncio
import hashlib
import logging
import os
import threading
from typing import Optional

from dotenv import find_dotenv, load_dotenv
from v2.nacos import (
    ClientConfig,
    ClientConfigBuilder,
    NacosConfigService,
    NacosNamingService,
)
from v2.nacos.ai.nacos_ai_service import NacosAIService

logger = logging.getLogger(__name__)


class NacosServiceManager:
    """Global Nacos Service Manager (Singleton Pattern).
    
    Features:
    - Auto-load global config from environment variables
    - Reuse same service instances for identical ClientConfig
    - Thread-safe connection pool management
    - Unified resource cleanup
    
    Service Pool Structure:
    {
        "config_hash_1": {
            "config": ClientConfig,
            "naming": NacosNamingService,
            "config": NacosConfigService,
            "ai": NacosAIService,
        },
        "config_hash_2": { ... }
    }
    """
    
    # Singleton instance
    _instance: Optional['NacosServiceManager'] = None
    _instance_lock = threading.Lock()
    
    def __new__(cls):
        """Singleton pattern: Ensure only one instance"""
        if cls._instance is None:
            with cls._instance_lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        """Initialize service manager"""
        # Avoid duplicate initialization
        if self._initialized:
            return
        
        # Global config (lazy loading)
        self._global_config: Optional[ClientConfig] = None
        self._global_config_loaded = False
        self._global_config_manually_set = False  # Flag for manual setting
        
        # Service pool: {config_hash: {service_type: service_instance}}
        self._service_pool: dict[str, dict] = {}
        
        # Async locks
        self._init_lock = asyncio.Lock()
        self._service_locks: dict[str, asyncio.Lock] = {}
        
        # Mark as initialized
        self._initialized = True
        
        logger.info("NacosServiceManager initialized (singleton)")
    
    # ==================== Configuration Management ====================
    
    @classmethod
    def load_config_from_env(cls) -> ClientConfig:
        """Load ClientConfig from environment variables.
        
        Automatically loads .env or .env.example file if present.
        
        Required environment variables:
            NACOS_SERVER_ADDR (or NACOS_SERVER_ADDRESS for backward compatibility)
            NACOS_NAMESPACE_ID (optional, default: "public")
        
        Optional environment variables (choose one):
            NACOS_ACCESS_KEY + NACOS_SECRET_KEY  (Alibaba Cloud MSE)
            NACOS_USERNAME + NACOS_PASSWORD      (Local Nacos)
        
        Returns:
            ClientConfig: Configuration object
            
        Raises:
            ValueError: If required environment variables are missing
        """
        # Load .env or .env.example if present
        dotenv_path = find_dotenv(raise_error_if_not_found=False)
        if dotenv_path:
            load_dotenv(dotenv_path, override=False)
            logger.debug(f"Loaded .env file from: {dotenv_path}")
        else:
            # Try .env.example as fallback
            if os.path.exists(".env.example"):
                load_dotenv(".env.example", override=False)
                logger.debug("Loaded .env.example file")
        
        # Support both NACOS_SERVER_ADDR and NACOS_SERVER_ADDRESS for compatibility
        server_address = os.getenv("NACOS_SERVER_ADDR") or os.getenv("NACOS_SERVER_ADDRESS")
        namespace_id = os.getenv("NACOS_NAMESPACE_ID", "public")
        
        if not server_address:
            raise ValueError(
                "Missing required environment variable: NACOS_SERVER_ADDR\n"
                "Please set it to your Nacos server address, e.g., 'localhost:8848'"
            )
        
        builder = ClientConfigBuilder()
        builder.server_address(server_address)
        builder.namespace_id(namespace_id)
        
        # Alibaba Cloud MSE authentication (priority)
        access_key = os.getenv("NACOS_ACCESS_KEY")
        secret_key = os.getenv("NACOS_SECRET_KEY")
        if access_key and secret_key:
            builder.access_key(access_key)
            builder.secret_key(secret_key)
            logger.info(f"Loaded Nacos config from env (MSE auth): {server_address}")
        else:
            # Local Nacos authentication (fallback)
            username = os.getenv("NACOS_USERNAME", "nacos")
            password = os.getenv("NACOS_PASSWORD", "nacos")
            builder.username(username)
            builder.password(password)
            logger.info(f"Loaded Nacos config from env (basic auth): {server_address}")
        
        # Optional configuration
        log_level = os.getenv("NACOS_LOG_LEVEL", "INFO")
        builder.log_level(log_level)
        
        return builder.build()
    
    def _get_global_config(self) -> ClientConfig:
        """Get global configuration (lazy loading).
        
        Priority:
        1. Manually set config (via set_global_config)
        2. Environment variable config
        """
        if not self._global_config_loaded:
            # If not manually set, load from environment variables
            if not self._global_config_manually_set:
                try:
                    self._global_config = self.load_config_from_env()
                    logger.info("Loaded global config from environment variables")
                except ValueError as e:
                    logger.error(f"Failed to load global config from env: {e}")
                    raise
            self._global_config_loaded = True
        return self._global_config
    
    @classmethod
    def set_global_config(cls, config: ClientConfig) -> None:
        """Set global configuration (manual mode).
        
        This method allows users to explicitly set global config, which takes precedence over environment variables.
        Should be called before creating any services.
        
        Args:
            config: ClientConfig object
            
        Example:
            >>> from v2.nacos import ClientConfigBuilder
            >>> config = (ClientConfigBuilder()
            ...     .server_address("localhost:8848")
            ...     .namespace_id("public")
            ...     .username("nacos")
            ...     .password("nacos")
            ...     .build())
            >>> NacosServiceManager.set_global_config(config)
        """
        manager = cls()
        manager._global_config = config
        manager._global_config_manually_set = True
        manager._global_config_loaded = True
        logger.info(
            f"Global config set manually: {config.server_list} / {config.namespace_id}"
        )
    
    @classmethod
    def get_global_config(cls) -> ClientConfig:
        """Get global configuration (class method).
        
        Returns:
            ClientConfig: Global configuration object
            
        Raises:
            ValueError: If not manually set and environment variables are missing
        """
        manager = cls()
        return manager._get_global_config()
    
    @classmethod
    def reset_global_config(cls) -> None:
        """Reset global configuration.
        
        Clears manually set config, will reload from environment variables next time.
        """
        manager = cls()
        manager._global_config = None
        manager._global_config_loaded = False
        manager._global_config_manually_set = False
        logger.info("Global config reset")
    
    # ==================== Hash and Lock Management ====================
    
    def _get_config_hash(self, config: ClientConfig) -> str:
        """Generate unique hash for ClientConfig.
        
        Generates a hash based on connection-identifying fields to ensure
        identical configurations reuse the same service instances.
        
        Based on the following fields:
        - server_list: List of Nacos server addresses
        - endpoint: Nacos endpoint (if using endpoint mode)
        - namespace_id: Nacos namespace identifier
        - context_path: Nacos context path
        - access_key: MSE authentication key (via credentials_provider)
        - username: Basic authentication username (for local Nacos)
        
        Note: Sensitive fields (secret_key, password) are NOT included in the hash
        as they are not needed for connection identification.
        
        Args:
            config: ClientConfig object
            
        Returns:
            str: 16-character hash string
        """
        # Get server list (sorted for consistency)
        server_list = config.server_list if hasattr(config, 'server_list') else []
        server_str = ",".join(sorted(server_list)) if server_list else ""
        
        # Get endpoint if available
        endpoint = getattr(config, 'endpoint', None) or ""
        
        # Get access_key from credentials_provider if available
        access_key = ""
        if hasattr(config, 'credentials_provider') and config.credentials_provider:
            credentials = config.credentials_provider.get_credentials()
            if credentials:
                access_key = credentials.access_key_id or ""
        
        # Build hash from connection-identifying fields
        hash_parts = [
            str(server_str),
            str(endpoint),
            str(config.namespace_id) if config.namespace_id else "",
            str(getattr(config, 'context_path', None) or ""),
            str(access_key),
            str(getattr(config, 'username', None) or ""),
        ]
        
        hash_string = "|".join(hash_parts)
        hash_value = hashlib.md5(hash_string.encode()).hexdigest()[:16]
        
        logger.debug(
            f"Generated config hash {hash_value} for "
            f"servers={server_str or endpoint}, namespace={config.namespace_id}"
        )
        
        return hash_value
    
    def _get_lock(self, config_hash: str) -> asyncio.Lock:
        """Get lock for config (lazy creation)"""
        if config_hash not in self._service_locks:
            self._service_locks[config_hash] = asyncio.Lock()
        return self._service_locks[config_hash]
    
    # ==================== Service Retrieval ====================
    
    async def get_naming_service(
        self,
        client_config: Optional[ClientConfig] = None,
    ) -> NacosNamingService:
        """Get NacosNamingService.
        
        Args:
            client_config: Optional custom config, uses global config if None
            
        Returns:
            NacosNamingService: Naming service instance (reuses existing connection)
        """
        # 1. Determine which config to use
        config = client_config if client_config else self._get_global_config()
        config_hash = self._get_config_hash(config)
        
        # 2. Check if service pool already has this config group
        if config_hash not in self._service_pool:
            async with self._get_lock(config_hash):
                if config_hash not in self._service_pool:
                    self._service_pool[config_hash] = {"config": config}
                    logger.info(f"Created service group for config hash: {config_hash}")
        
        # 3. Check if this config group has naming service
        service_group = self._service_pool[config_hash]
        if "naming" not in service_group:
            async with self._get_lock(config_hash):
                if "naming" not in service_group:
                    logger.info(f"Creating NacosNamingService for hash: {config_hash}")
                    service_group["naming"] = await NacosNamingService.create_naming_service(config)
                    logger.info(f"NacosNamingService created for hash: {config_hash}")
        
        return service_group["naming"]
    
    async def get_config_service(
        self,
        client_config: Optional[ClientConfig] = None,
    ) -> NacosConfigService:
        """Get NacosConfigService.
        
        Args:
            client_config: Optional custom config, uses global config if None
            
        Returns:
            NacosConfigService: Config service instance (reuses existing connection)
        """
        config = client_config if client_config else self._get_global_config()
        config_hash = self._get_config_hash(config)
        
        if config_hash not in self._service_pool:
            async with self._get_lock(config_hash):
                if config_hash not in self._service_pool:
                    self._service_pool[config_hash] = {}
                    logger.info(f"Created service group for config hash: {config_hash}")
        
        service_group = self._service_pool[config_hash]
        if "config" not in service_group:
            async with self._get_lock(config_hash):
                if "config" not in service_group:
                    logger.info(f"Creating NacosConfigService for hash: {config_hash}")
                    service_group["config"] = await NacosConfigService.create_config_service(config)
                    logger.info(f"NacosConfigService created for hash: {config_hash}")
        
        return service_group["config"]
    
    async def get_ai_service(
        self,
        client_config: Optional[ClientConfig] = None,
    ) -> NacosAIService:
        """Get NacosAIService.
        
        Args:
            client_config: Optional custom config, uses global config if None
            
        Returns:
            NacosAIService: AI service instance (reuses existing connection)
        """
        config = client_config if client_config else self._get_global_config()
        config_hash = self._get_config_hash(config)
        
        if config_hash not in self._service_pool:
            async with self._get_lock(config_hash):
                if config_hash not in self._service_pool:
                    self._service_pool[config_hash] = {"config": config}
                    logger.info(f"Created service group for config hash: {config_hash}")
        
        service_group = self._service_pool[config_hash]
        if "ai" not in service_group:
            async with self._get_lock(config_hash):
                if "ai" not in service_group:
                    logger.info(f"Creating NacosAIService for hash: {config_hash}")
                    service_group["ai"] = await NacosAIService.create_ai_service(config)
                    logger.info(f"NacosAIService created for hash: {config_hash}")
        
        return service_group["ai"]
    
    # ==================== Statistics and Management ====================
    
    @classmethod
    def get_stats(cls) -> dict:
        """Get service pool statistics.
        
        Returns:
            dict: Statistics information
            {
                "config_count": 2,
                "total_services": 6,
                "configs": [
                    {
                        "hash": "abc123",
                        "server": "localhost:8848",
                        "namespace": "public",
                        "services": ["naming", "config", "ai"]
                    }
                ]
            }
        """
        manager = cls()
        
        configs_info = []
        total_services = 0
        
        for config_hash, service_group in manager._service_pool.items():
            config = service_group.get("config")
            services = [k for k in service_group.keys() if k != "config"]
            total_services += len(services)
            
            configs_info.append({
                "hash": config_hash,
                "server": config.server_address if config else "unknown",
                "namespace": config.namespace_id if config else "unknown",
                "services": services,
            })
        
        return {
            "config_count": len(manager._service_pool),
            "total_services": total_services,
            "configs": configs_info,
        }
    
    @classmethod
    async def cleanup(cls) -> None:
        """Clean up all service connections.
        
        Note: Service instances need to be re-obtained after cleanup
        """
        manager = cls()
        
        logger.info("Cleaning up NacosServiceManager...")
        
        # Clean up all services
        cleanup_count = 0
        for config_hash, service_group in manager._service_pool.items():
            for service_type in ["ai", "config", "naming"]:
                if service_type in service_group:
                    service = service_group[service_type]
                    try:
                        # Try to call cleanup method (if available)
                        if hasattr(service, "close"):
                            await service.close()
                        elif hasattr(service, "shutdown"):
                            await service.shutdown()
                        cleanup_count += 1
                    except Exception as e:
                        logger.warning(
                            f"Failed to cleanup {service_type} service "
                            f"for hash {config_hash}: {e}"
                        )
        
        # Clear service pool
        manager._service_pool.clear()
        manager._service_locks.clear()
        
        logger.info(f"Cleaned up {cleanup_count} services")
    
    @classmethod
    def is_initialized(cls) -> bool:
        """Check if manager is initialized"""
        return cls._instance is not None and cls._instance._initialized


# ==================== Convenience Functions ====================

async def get_nacos_naming_service(
    client_config: Optional[ClientConfig] = None,
) -> NacosNamingService:
    """Convenience function: Get NacosNamingService"""
    manager = NacosServiceManager()
    return await manager.get_naming_service(client_config)


async def get_nacos_config_service(
    client_config: Optional[ClientConfig] = None,
) -> NacosConfigService:
    """Convenience function: Get NacosConfigService"""
    manager = NacosServiceManager()
    return await manager.get_config_service(client_config)


async def get_nacos_ai_service(
    client_config: Optional[ClientConfig] = None,
) -> NacosAIService:
    """Convenience function: Get NacosAIService"""
    manager = NacosServiceManager()
    return await manager.get_ai_service(client_config)

