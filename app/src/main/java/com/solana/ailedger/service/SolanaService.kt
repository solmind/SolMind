package com.solana.ailedger.service

import android.util.Log
import com.solana.ailedger.data.model.SolanaTransaction
import com.solana.ailedger.data.model.SolanaTransactionType
import com.solana.ailedger.data.model.TokenInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaService @Inject constructor() {
    
    private val rpcUrl = "https://api.mainnet-beta.solana.com"
    private val tag = "SolanaService"
    
    suspend fun getAccountTransactions(
        address: String,
        limit: Int = 50
    ): List<SolanaTransaction> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getSignaturesForAddress")
                put("params", JSONArray().apply {
                    put(address)
                    put(JSONObject().apply {
                        put("limit", limit)
                    })
                })
            }
            
            val response = makeRpcCall(requestBody)
            val signatures = response.getJSONArray("result")
            
            val transactions = mutableListOf<SolanaTransaction>()
            
            for (i in 0 until signatures.length()) {
                val sigInfo = signatures.getJSONObject(i)
                val signature = sigInfo.getString("signature")
                
                try {
                    val transaction = getTransactionDetails(signature, address)
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error getting transaction details for $signature", e)
                }
            }
            
            transactions
        } catch (e: Exception) {
            Log.e(tag, "Error getting account transactions", e)
            emptyList()
        }
    }
    
    private suspend fun getTransactionDetails(
        signature: String,
        userAddress: String
    ): SolanaTransaction? = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getTransaction")
                put("params", JSONArray().apply {
                    put(signature)
                    put(JSONObject().apply {
                        put("encoding", "json")
                        put("maxSupportedTransactionVersion", 0)
                    })
                })
            }
            
            val response = makeRpcCall(requestBody)
            val result = response.getJSONObject("result")
            
            if (result.isNull("transaction")) {
                return@withContext null
            }
            
            val transaction = result.getJSONObject("transaction")
            val message = transaction.getJSONObject("message")
            val accountKeys = message.getJSONArray("accountKeys")
            val instructions = message.getJSONArray("instructions")
            
            val blockTime = if (result.has("blockTime") && !result.isNull("blockTime")) {
                result.getLong("blockTime")
            } else null
            
            val slot = result.getLong("slot")
            val fee = result.getJSONObject("meta").getLong("fee").toDouble() / 1_000_000_000 // Convert lamports to SOL
            
            // Parse pre and post balances to determine amount
            val meta = result.getJSONObject("meta")
            val preBalances = meta.getJSONArray("preBalances")
            val postBalances = meta.getJSONArray("postBalances")
            
            var amount = 0.0
            var fromAddress: String? = null
            var toAddress: String? = null
            
            // Find user's account index
            var userAccountIndex = -1
            for (i in 0 until accountKeys.length()) {
                if (accountKeys.getString(i) == userAddress) {
                    userAccountIndex = i
                    break
                }
            }
            
            if (userAccountIndex >= 0) {
                val preBalance = preBalances.getLong(userAccountIndex).toDouble() / 1_000_000_000
                val postBalance = postBalances.getLong(userAccountIndex).toDouble() / 1_000_000_000
                amount = kotlin.math.abs(postBalance - preBalance)
                
                if (postBalance > preBalance) {
                    // Received money
                    toAddress = userAddress
                    // Find sender (account with decreased balance)
                    for (i in 0 until accountKeys.length()) {
                        if (i != userAccountIndex) {
                            val otherPreBalance = preBalances.getLong(i).toDouble()
                            val otherPostBalance = postBalances.getLong(i).toDouble()
                            if (otherPreBalance > otherPostBalance) {
                                fromAddress = accountKeys.getString(i)
                                break
                            }
                        }
                    }
                } else {
                    // Sent money
                    fromAddress = userAddress
                    // Find receiver (account with increased balance)
                    for (i in 0 until accountKeys.length()) {
                        if (i != userAccountIndex) {
                            val otherPreBalance = preBalances.getLong(i).toDouble()
                            val otherPostBalance = postBalances.getLong(i).toDouble()
                            if (otherPostBalance > otherPreBalance) {
                                toAddress = accountKeys.getString(i)
                                break
                            }
                        }
                    }
                }
            }
            
            // Determine transaction type
            val transactionType = determineTransactionType(instructions, accountKeys)
            
            // Extract memo if present
            var memo: String? = null
            for (i in 0 until instructions.length()) {
                val instruction = instructions.getJSONObject(i)
                val programIdIndex = instruction.getInt("programIdIndex")
                val programId = accountKeys.getString(programIdIndex)
                
                if (programId == "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr") {
                    // This is a memo instruction
                    val data = instruction.getString("data")
                    memo = String(android.util.Base64.decode(data, android.util.Base64.DEFAULT))
                    break
                }
            }
            
            SolanaTransaction(
                signature = signature,
                blockTime = blockTime,
                slot = slot,
                amount = amount,
                fee = fee,
                fromAddress = fromAddress,
                toAddress = toAddress,
                type = transactionType,
                programId = if (instructions.length() > 0) {
                    val firstInstruction = instructions.getJSONObject(0)
                    val programIdIndex = firstInstruction.getInt("programIdIndex")
                    accountKeys.getString(programIdIndex)
                } else null,
                memo = memo
            )
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing transaction details", e)
            null
        }
    }
    
    private fun determineTransactionType(
        instructions: JSONArray,
        accountKeys: JSONArray
    ): SolanaTransactionType {
        if (instructions.length() == 0) return SolanaTransactionType.UNKNOWN
        
        try {
            val firstInstruction = instructions.getJSONObject(0)
            val programIdIndex = firstInstruction.getInt("programIdIndex")
            val programId = accountKeys.getString(programIdIndex)
            
            return when (programId) {
                "11111111111111111111111111111111" -> SolanaTransactionType.TRANSFER
                "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA" -> SolanaTransactionType.TOKEN_TRANSFER
                "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM" -> SolanaTransactionType.SWAP // Jupiter
                "Stake11111111111111111111111111111111111111" -> SolanaTransactionType.STAKE
                else -> {
                    // Check for NFT operations
                    if (programId.contains("metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt")) {
                        SolanaTransactionType.NFT_MINT
                    } else {
                        SolanaTransactionType.PROGRAM_INTERACTION
                    }
                }
            }
        } catch (e: Exception) {
            return SolanaTransactionType.UNKNOWN
        }
    }
    
    suspend fun validateSolanaAddress(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Basic validation - Solana addresses are 32-44 characters long and base58 encoded
            if (address.length !in 32..44) return@withContext false
            
            // Check if address exists on blockchain
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getAccountInfo")
                put("params", JSONArray().apply {
                    put(address)
                })
            }
            
            val response = makeRpcCall(requestBody)
            // If we get a response without error, the address format is valid
            !response.has("error")
        } catch (e: Exception) {
            Log.e(tag, "Error validating Solana address", e)
            false
        }
    }
    
    private suspend fun makeRpcCall(requestBody: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(rpcUrl)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        
        connection.outputStream.use { os ->
            os.write(requestBody.toString().toByteArray())
        }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response)
        } else {
            throw Exception("HTTP error code: $responseCode")
        }
    }
}