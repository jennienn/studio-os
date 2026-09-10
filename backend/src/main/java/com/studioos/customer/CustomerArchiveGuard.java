package com.studioos.customer;
import java.util.UUID;
/** Later common domains participate in the same archive transaction. */
public interface CustomerArchiveGuard { void validateArchive(UUID studioId,UUID customerId); }
