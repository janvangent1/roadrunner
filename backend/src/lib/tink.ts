import { aead, binaryInsecure } from 'tink-crypto';
import { Aead } from 'tink-crypto/aead/internal/aead';

let keysetHandle: Awaited<ReturnType<typeof binaryInsecure.deserializeKeyset>> | null = null;

/**
 * Initialize Tink with the keyset from the TINK_KEYSET_JSON env var.
 * TINK_KEYSET_JSON is a base64-encoded binary Tink keyset (produced by
 * binaryInsecure.serializeKeyset + base64 encoding).
 * Call once at app startup before any encrypt/decrypt calls.
 *
 * To generate a keyset for development:
 *   node -e "
 *     const { aead, binaryInsecure } = require('tink-crypto');
 *     aead.register();
 *     aead.generateNew(aead.aes256GcmKeyTemplate()).then(h => {
 *       console.log(Buffer.from(binaryInsecure.serializeKeyset(h)).toString('base64'));
 *     });
 *   "
 */
export async function initTink(): Promise<void> {
  // Register AES-GCM key manager (idempotent — safe to call multiple times)
  aead.register();

  const envVal = process.env.TINK_KEYSET_JSON;
  if (!envVal) throw new Error('TINK_KEYSET_JSON env var is required');

  const keysetBinary = Buffer.from(envVal, 'base64');
  keysetHandle = binaryInsecure.deserializeKeyset(keysetBinary);
}

/**
 * Encrypt a GPX buffer in memory. Returns encrypted bytes.
 * NEVER writes plaintext to disk. The input Buffer is encrypted
 * entirely in memory and the encrypted Buffer is returned.
 *
 * @param plaintext - Raw GPX file bytes
 * @param associatedData - Optional AAD (e.g. route ID as UTF-8 bytes)
 */
export async function encryptGpx(plaintext: Buffer, associatedData?: Buffer): Promise<Buffer> {
  if (!keysetHandle) throw new Error('Tink not initialized — call initTink() first');
  const primitive = await keysetHandle.getPrimitive<Aead>(Aead);
  const encrypted = await primitive.encrypt(
    new Uint8Array(plaintext),
    associatedData ? new Uint8Array(associatedData) : new Uint8Array(0),
  );
  return Buffer.from(encrypted);
}

/**
 * Decrypt an encrypted GPX buffer in memory. Returns plaintext bytes.
 *
 * @param ciphertext - Encrypted bytes (produced by encryptGpx)
 * @param associatedData - Optional AAD — must match what was passed to encryptGpx
 */
export async function decryptGpx(ciphertext: Buffer, associatedData?: Buffer): Promise<Buffer> {
  if (!keysetHandle) throw new Error('Tink not initialized — call initTink() first');
  const primitive = await keysetHandle.getPrimitive<Aead>(Aead);
  const decrypted = await primitive.decrypt(
    new Uint8Array(ciphertext),
    associatedData ? new Uint8Array(associatedData) : new Uint8Array(0),
  );
  return Buffer.from(decrypted);
}
