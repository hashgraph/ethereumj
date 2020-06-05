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
import org.ethereum.datasource.CachedSource;
import org.ethereum.datasource.MultiCache;
import org.ethereum.datasource.SimplifiedWriteCache;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.WriteCache;
import org.ethereum.vm.DataWord;

import java.math.BigInteger;

public class ServicesRepositoryImpl extends RepositoryImpl {
	protected ServicesRepositoryImpl parent;

	protected ServicesRepositoryImpl() {
		super();
	}

	public ServicesRepositoryImpl(
			Source<byte[], AccountState> accountStateCache,
			Source<byte[], byte[]> codeCache,
			MultiCache<? extends CachedSource<DataWord, DataWord>> storageCache
	) {
		super.init(accountStateCache, codeCache, storageCache);
	}

	public MultiCache<? extends CachedSource<DataWord, DataWord>> getStorageCache() {
		return storageCache;
	}

	@Override
	public synchronized AccountState createAccount(byte[] addr) {
		AccountState state = new AccountState(BigInteger.ZERO, BigInteger.ZERO);
		accountStateCache.put(addr, state);
		return state;
	}

	@Override
	public synchronized ServicesRepositoryImpl startTracking() {
		Source<byte[], byte[]> trackCodeCache = new SimplifiedWriteCache.BytesKey<>(codeCache);
		Source<byte[], AccountState> trackAccountStateCache = new SimplifiedWriteCache.BytesKey<>(accountStateCache);
		MultiCache<CachedSource<DataWord, DataWord>> trackStorageCache = new MultiCache(storageCache) {
			@Override
			protected CachedSource create(byte[] key, CachedSource srcCache) {
				return new WriteCache<>(srcCache, WriteCache.CacheType.SIMPLE);
			}
		};

		ServicesRepositoryImpl ret = new ServicesRepositoryImpl(trackAccountStateCache, trackCodeCache, trackStorageCache);
		ret.parent = this;

		return ret;
	}

	public long getExpirationTime(byte[] addr) {
		AccountState accountState = getOrCreateAccountState(addr);
		return accountState == null ? 0l : accountState.getExpirationTime();
	}

	public void setExpirationTime(byte[] addr, long expirationTime) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setExpirationTime(expirationTime);
		accountStateCache.put(addr, accountState);
	}

	public synchronized void setReceiverSigRequired(byte[] addr, boolean receiverSigRequired) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setReceiverSigRequired(receiverSigRequired);
		accountStateCache.put(addr, accountState);
	}

	public synchronized void setAccountNum(byte[] addr, long accountNum) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setAccountNum(accountNum);
		accountStateCache.put(addr, accountState);
	}

	public synchronized void setRealmId(byte[] addr, long realmId) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setRealmId(realmId);
		accountStateCache.put(addr, accountState);
	}

	public synchronized void setShardId(byte[] addr, long shardId) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setShardId(shardId);
		accountStateCache.put(addr, accountState);
	}

	public long getAutoRenewPeriod(byte[] addr) {
		AccountState accountState = getOrCreateAccountState(addr);
		return accountState == null ? 0l : accountState.getAutoRenewPeriod();
	}

	public void setAutoRenewPeriod(byte[] addr, long autoRenewPeriod) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setAutoRenewPeriod(autoRenewPeriod);
		accountStateCache.put(addr, accountState);
	}

	public synchronized AccountState getAccount(byte[] addr) {
		return getAccountState(addr);
	}

	public void setCreateTimeMs(byte[] addr, long createTimeMs) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setCreateTimeMs(createTimeMs);
		accountStateCache.put(addr, accountState);
	}

	public void setDeleted(byte[] addr, boolean deleted) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setDeleted(deleted);
		accountStateCache.put(addr, accountState);
	}

	public boolean isDeleted(byte[] addr) {
		AccountState accountState = getOrCreateAccountState(addr);
		return accountState == null ? false : accountState.isDeleted();
	}

	public void setSmartContract(byte[] addr, boolean smartContract) {
		AccountState accountState = getOrCreateAccountState(addr);
		accountState.setSmartContract(smartContract);
		accountStateCache.put(addr, accountState);
	}

	public boolean isSmartContract(byte[] addr) {
		AccountState accountState = getOrCreateAccountState(addr);
		return accountState == null ? false : accountState.isSmartContract();
	}
}
