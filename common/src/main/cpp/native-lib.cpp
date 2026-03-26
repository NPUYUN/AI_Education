#include <jni.h>
#include <string>

// Simple XOR decryption to hide the key from simple strings command
// The key should be passed from BuildConfig during compilation or hardcoded XOR
// For demonstration, we hardcode a simple XORed key here.
// Real API Key: "sk-your-real-api-key"
// Let's assume the key is passed or we just return a placeholder for now.

// Helper macro to convert to string
#define STR(x) #x
#define XSTR(x) STR(x)

// Simple XOR encryption/decryption function
std::string xor_crypt(const std::string& input) {
    std::string output = input;
    // Simple 8-bit key for XOR, in real scenario use a more complex sequence
    char key = 0x5A; 
    for (size_t i = 0; i < output.size(); ++i) {
        output[i] ^= key;
    }
    return output;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_common_utils_NativeLib_getApiKeyNative(
        JNIEnv* env,
        jobject /* this */) {
    // In a real scenario, you would store the XOR encrypted key here.
    // For demonstration, we simulate the decryption process.
    
#ifdef API_KEY
    std::string api_key = XSTR(API_KEY);
#else
    std::string api_key = "sk-placeholder-key-from-ndk";
#endif

    // Example of how it would be used if we had encrypted key:
    // std::string encrypted_key = { ... byte array ... };
    // std::string decrypted_key = xor_crypt(encrypted_key);
    // return env->NewStringUTF(decrypted_key.c_str());

    return env->NewStringUTF(api_key.c_str());
}
