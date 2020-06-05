/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.db;

import org.ethereum.core.AccountState;
import org.ethereum.datasource.AbstractCachedSource;
import org.ethereum.datasource.MultiCache;
import org.ethereum.datasource.ReadWriteCache;
import org.ethereum.datasource.SimplifiedWriteCache;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.StoragePersistence;
import org.ethereum.datasource.WriteCache;
import org.ethereum.datasource.WriteCache.CacheEntry;
import org.ethereum.datasource.WriteCache.CacheType;
import org.ethereum.vm.DataWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServicesRepositoryRoot extends ServicesRepositoryImpl {
	private static final int DWORD_BYTES = 32;

	private StoragePersistence storagePersistence;

	public ServicesRepositoryRoot(
			final Source<byte[], AccountState> accountStateSource,
			final Source<byte[], byte[]> codeSource
	) {
		Source<byte[], byte[]> codeCache = new SimplifiedWriteCache.BytesKey<>(codeSource);
		Source<byte[], AccountState> accountStateCache = new SimplifiedWriteCache.BytesKey<>(accountStateSource);
		MultiCache<StorageCache> storageCache = new MultiStorageCache();
		init(accountStateCache, codeCache, storageCache);
	}

	@Override
	public synchronized void flush() {
		commit();
	}

	public void emptyStorageCache() {
		storageCache = new MultiStorageCache();
	}

	public void setStoragePersistence(StoragePersistence storagePersistence) {
		this.storagePersistence = storagePersistence;
	}

	public boolean flushStorageCacheIfTotalSizeLessThan(int maxStorageKb) {
		long start = System.currentTimeMillis();
		Map<byte[], byte[]> cachesToPersist = storageCache.getSerializedCache();
		if (cachesToPersist != null) {
			ArrayList<byte[]> addresses = new ArrayList<>(cachesToPersist.keySet());
			addresses.sort((left, right) -> {
				for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
					int a = (left[i] & 0xff);
					int b = (right[j] & 0xff);
					if (a != b) {
						return a - b;
					}
				}
				return left.length - right.length;
			});
			int totalSize = 0;
			for (byte[] currAddress : addresses) {
				totalSize += cachesToPersist.get(currAddress).length;
				if (totalSize > (1024 * maxStorageKb)) {
					return false;
				}
			}
			for (byte[] address : addresses) {
				AccountState currAccount = getAccount(address);
				long expirationTime = currAccount.getExpirationTime();
				long createTime = currAccount.getCreateTimeMs();
				long before = System.currentTimeMillis();
				this.storagePersistence.persist(address, cachesToPersist.get(address), expirationTime, createTime);
				long after = System.currentTimeMillis();
			}
		}
		emptyStorageCache();
		long end = System.currentTimeMillis();

		return true;
	}

	private static class StorageCache extends ReadWriteCache<DataWord, DataWord> {
		StorageCache() {
			super(null, CacheType.SIMPLE);
		}

		StorageCache(WriteCache<DataWord, DataWord> writeCache) {
			super(null, writeCache);
		}

		WriteCache<DataWord, DataWord> getWriteCache() {
			return writeCache;
		}
	}

	private class MultiStorageCache extends MultiCache<StorageCache> {
		MultiStorageCache() {
			super(null);
		}

		@Override
		public synchronized boolean flushImpl() {
			return false;
		}

		@Override
		protected synchronized StorageCache create(byte[] key, StorageCache srcCache) {
			return new StorageCache();
		}

		@Override
		public synchronized StorageCache get(byte[] key) {
			AbstractCachedSource.Entry<StorageCache> ownCacheEntry = getCached(key);
			StorageCache ownCache = ownCacheEntry == null ? null : ownCacheEntry.value();

			if (ownCache == null) {
				if (storagePersistence.storageExist(key)) {
					long start = System.currentTimeMillis();
					byte[] previouslyPersisted = storagePersistence.get(key);
					WriteCache<DataWord, DataWord> cacheToPut = deserializeCacheMap(previouslyPersisted);
					ownCache = new StorageCache(cacheToPut);
					long end = System.currentTimeMillis();
				} else {
					ownCache = create(key, null);
				}
				put(key, ownCache);
			}

			return ownCache;
		}

		@Override
		public synchronized Map<byte[], byte[]> getSerializedCache() {
			Map<byte[], byte[]> serializedCache = new HashMap<>();
			for (byte[] currAddress : writeCache.getCache().keySet()) {
				StorageCache storageCache = get(currAddress);
				if (storageCache.getWriteCache().hasModified()) {
					Map<DataWord, CacheEntry<DataWord>> underlyingCache = storageCache.getWriteCache().getCache();
					byte[] serializedCacheMap = serializeCacheMap(underlyingCache);
					serializedCache.put(currAddress, serializedCacheMap);
				}
			}
			return serializedCache;
		}

		private byte[] serializeCacheMap(Map<DataWord, CacheEntry<DataWord>> cacheMap) {
			ArrayList<DataWord> keys = new ArrayList<>(cacheMap.keySet());
			Collections.sort(keys);
			int offset = 0;
			int skips = 0;
			byte[] result = new byte[keys.size() * DWORD_BYTES * 2];
			for (DataWord key : keys) {
				CacheEntry<DataWord> currEntry = cacheMap.get(key);
				if (currEntry != null && currEntry.value() != null && currEntry.value().getData() != null) {
					System.arraycopy(key.getData(), 0, result, offset, DWORD_BYTES);
					offset += DWORD_BYTES;
					System.arraycopy(cacheMap.get(key).value().getData(), 0, result, offset, DWORD_BYTES);
					offset += DWORD_BYTES;
				} else {
					skips += 1;
				}
			}

			if (skips > 0) {
				int newSize = (keys.size() - skips) * DWORD_BYTES * 2;
				byte[] newResult = new byte[newSize];
				System.arraycopy(result, 0, newResult, 0, newSize);
				return newResult;
			}

			return result;
		}

		private WriteCache<DataWord, DataWord> deserializeCacheMap(byte[] serializedMap) {
			WriteCache<DataWord, DataWord> cacheToPut = new WriteCache<DataWord, DataWord>(null, CacheType.SIMPLE);

			int offset = 0;
			while (offset < serializedMap.length) {
				byte[] keyBytes = new byte[DWORD_BYTES];
				byte[] valBytes = new byte[DWORD_BYTES];
				System.arraycopy(serializedMap, offset, keyBytes, 0, DWORD_BYTES);
				offset += DWORD_BYTES;
				System.arraycopy(serializedMap, offset, valBytes, 0, DWORD_BYTES);
				offset += DWORD_BYTES;
				cacheToPut.put(new DataWord(keyBytes), new DataWord(valBytes));
			}
			cacheToPut.resetModified();
			return cacheToPut;
		}
	}
}
