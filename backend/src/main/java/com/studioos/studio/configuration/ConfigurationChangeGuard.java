package com.studioos.studio.configuration;
import java.util.UUID;
public interface ConfigurationChangeGuard {void validate(UUID studio,ConfigurationDto.Command command);}
