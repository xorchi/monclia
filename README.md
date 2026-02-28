# Monclia

**Monero Command Line Interface Android**

A native Android application that provides an identical experience to `monero-wallet-cli` on Linux — without Termux, without dependencies on external environments.

## Goals

- Full CLI wallet interface, identical to official `monero-wallet-cli`
- Wallet files, keys, and shared ring DB fully interoperable with Linux builds
- All traffic routed through Tor by default
- Connects to remote nodes only (no local daemon)
- No Google Play — distributed as signed APK

## Default Nodes

Five community-recommended nodes (all traffic via Tor):

| Operator | Address | Port |
|---|---|---|
| selsta (Monero core dev) | selsta1.featherwallet.net | 18081 |
| sethforprivacy | node.sethforprivacy.com | 18089 |
| stormycloud | xmr.stormycloud.org | 18089 |
| trocador | node.trocador.app | 18089 |
| boldsuck (.onion) | 6dsdenp55q7wqtbx7mqmrnf4jskoyahadpg6mxtqp26klsqhqqzsbad.onion | 18089 |

## Architecture

See [docs/architecture.md](docs/architecture.md).

## Build

See [docs/building.md](docs/building.md).

## License

GPL-3.0
