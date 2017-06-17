import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    private UTXOPool utxoPool = null;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        
        Transaction.Input txInput = null;
        Transaction.Output txOutput = null;

        double totalInputValue = 0;
        double totalOutputValue = 0;

        // (1) For each output, check if it is in current utxoPool
        for(int index=0; index < txOutputs.size(); index++) {
          txOutput = txOutputs.get(index);
          
          // (4) Ensure all output values are non-negative
          if(txOutput.value < 0) return false;
          
          totalOutputValue += txOutput.value;
        }
        
        // (2) Check tx inputs are valid
        ArrayList<UTXO> utxoSeen = new ArrayList<UTXO>();
        
        for(int index=0; index < tx.numInputs(); index++) {
          txInput = txInputs.get(index);
          
          // I need the public key from the previous transaction
          // Where can I find it? In the UTXOPool
          // Construct the UTXO, then query utxopool
          UTXO prevUtxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
          
          if(!this.utxoPool.contains(prevUtxo)) return false;
          
          Transaction.Output prevOutput = this.utxoPool.getTxOutput(prevUtxo);
          
          if(!Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(index), txInput.signature)) return false;
        
          // (3) Check if prevUtxo has already been processed
          if(utxoSeen.contains(prevUtxo)) return false;
        
          // Add every input's UTXO into the seen list
          utxoSeen.add(prevUtxo);
          
          totalInputValue += prevOutput.value;
        }
        
        return (totalInputValue >= totalOutputValue);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        
        Transaction currentTx = null;
        
        for(int q=0; q < possibleTxs.length; q++) {
        
          currentTx = possibleTxs[q];
          
          if(isValidTx(currentTx)) {
          
            validTxs.add(currentTx);
          
            // If transaction is valid, UTXO pool must be updated.
            // Destroy the input UTXOs and add the output UTXOs
            ArrayList<Transaction.Input> txInputs = currentTx.getInputs();
            Transaction.Input txInput = null;
            
            ArrayList<Transaction.Output> txOutputs = currentTx.getOutputs();
            Transaction.Output txOutput = null;
            
            for(int index=0; index < txInputs.size(); index++) {
              txInput = txInputs.get(index);
              UTXO prevUtxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
              this.utxoPool.removeUTXO(prevUtxo);
            }
            
            for(int index=0; index < txOutputs.size(); index++) {
              txOutput = txOutputs.get(index);
              UTXO newUtxo = new UTXO(currentTx.getHash(), index);
              this.utxoPool.addUTXO(newUtxo, txOutput);
            } 
            
          }
        }
        
        Transaction[] valids = new Transaction[validTxs.size()];
        valids = validTxs.toArray(valids);
        
        return valids;    
    }

    /*
        To remove
     */
    public static void main(String[] args) {
      System.out.println("Going good");
    }
}
