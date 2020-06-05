package org.ethereum.datasource;

public interface StoragePersistence {
  byte[] get(byte[] address);
  boolean storageExist(byte[] address);
  void persist(byte[] address, byte[] storageCache, long expirationTime, long currentTime);
}
