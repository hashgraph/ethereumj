# EVM adapter for Hedera Smart Contract Services

The Hedera Smart Contract Service supports Solidity contracts 
with `pragma solidity <=0.5.9`, where the semantics of a 
transaction sent to a Hedera Solidity contract are largely
equivalent to the semantics of a transaction sent to an 
Ethereum smart contract. This equivalence derives from reuse
of the `ethereumj` EVM implementation, with a handful of
adaptations.

The adaptations are as follows:
  1. Extend the `org.ethereum.core.AccountState` type to include 
    properties used by the Hedera network.
  2. Suppress the loading and persistence of (key-scoped) contract 
    storage from the `org.ethereum.datasource.Source`s
    that back storage caches used by `org.ethereum.db.RepositoryRoot`; 
    instead, use an `org.ethereum.datasource.StoragePersistence` 
    to perform (contract-scoped) storage loading and persistence upon 
    creation of each storage cache.
  3. Modify the gas cost calculation for the `SSTORE` and 
    `LOG{0-4}` EVM opcodes to reflect the active Hedera resource 
    prices for disk and RAM, respectively.
  4. Generate new contract addresses using an injected 
    `org.ethereum.vm.program.NewAccountCreateAdapter` rather than 
    the hash of the creator address and nonce.
  5. Delegate the `CREATE2` opcode to the `CREATE` opcode (the
    semantics introduced with `CREATE2` are irrelevant given 
    the preceding item).
  6. Change `slf4j` binding to `org.apache.logging.log4j`.

For a complete view of the Hedera adaptations, please perform a 
`git diff` of this branch and the `1.12.0` tag.

# License
`ethereumj` is released under the [LGPL-V3 license](https://github.com/hashgraph/ethereumj/blob/develop/LICENSE).
