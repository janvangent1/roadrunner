// tink-crypto ships a browser UMD bundle that references `self`.
// Node.js doesn't define `self`, so we alias it to `global` here.
// This file must be loaded before tink-crypto via --require.
if (typeof self === 'undefined') {
  global.self = global;
}
