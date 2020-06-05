package org.ethereum.vm.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;

public class NewAccountCreateAdapter {
    private Map<byte[], List<byte[]>> createdContracts = new HashMap<>();

    public byte[] calculateNewAddress(byte[] ownerAddress , Repository track) {
        byte[] nonce = track.getNonce(ownerAddress).toByteArray();
        return HashUtil.calcNewAddr(ownerAddress, nonce);
    }

    public void addCreatedContract(byte[] newContractAddress, byte[] creatorAddress, Repository track) {
        List<byte[]> contractsForCreator = createdContracts.get(creatorAddress);
        if (contractsForCreator == null) {
            contractsForCreator = new ArrayList<byte[]>();
        }
        contractsForCreator.add(newContractAddress);
        createdContracts.put(creatorAddress, contractsForCreator);
    }
    
    public Map<byte[], List<byte[]>> getCreatedContracts() {
        return createdContracts;
    }
}
