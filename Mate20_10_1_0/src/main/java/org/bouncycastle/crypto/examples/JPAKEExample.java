package org.bouncycastle.crypto.examples;

import java.io.PrintStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound2Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound3Payload;
import org.bouncycastle.crypto.digests.SHA256Digest;

public class JPAKEExample {
    private static BigInteger deriveSessionKey(BigInteger bigInteger) {
        SHA256Digest sHA256Digest = new SHA256Digest();
        byte[] byteArray = bigInteger.toByteArray();
        byte[] bArr = new byte[sHA256Digest.getDigestSize()];
        sHA256Digest.update(byteArray, 0, byteArray.length);
        sHA256Digest.doFinal(bArr, 0);
        return new BigInteger(bArr);
    }

    public static void main(String[] strArr) throws CryptoException {
        JPAKEPrimeOrderGroup jPAKEPrimeOrderGroup = JPAKEPrimeOrderGroups.NIST_3072;
        BigInteger p = jPAKEPrimeOrderGroup.getP();
        BigInteger q = jPAKEPrimeOrderGroup.getQ();
        BigInteger g = jPAKEPrimeOrderGroup.getG();
        System.out.println("********* Initialization **********");
        System.out.println("Public parameters for the cyclic group:");
        PrintStream printStream = System.out;
        printStream.println("p (" + p.bitLength() + " bits): " + p.toString(16));
        PrintStream printStream2 = System.out;
        printStream2.println("q (" + q.bitLength() + " bits): " + q.toString(16));
        PrintStream printStream3 = System.out;
        printStream3.println("g (" + p.bitLength() + " bits): " + g.toString(16));
        PrintStream printStream4 = System.out;
        StringBuilder sb = new StringBuilder();
        sb.append("p mod q = ");
        sb.append(p.mod(q).toString(16));
        printStream4.println(sb.toString());
        PrintStream printStream5 = System.out;
        printStream5.println("g^{q} mod p = " + g.modPow(q, p).toString(16));
        System.out.println("");
        PrintStream printStream6 = System.out;
        printStream6.println("(Secret passwords used by Alice and Bob: \"" + "password" + "\" and \"" + "password" + "\")\n");
        SHA256Digest sHA256Digest = new SHA256Digest();
        SecureRandom secureRandom = new SecureRandom();
        JPAKEParticipant jPAKEParticipant = new JPAKEParticipant("alice", "password".toCharArray(), jPAKEPrimeOrderGroup, sHA256Digest, secureRandom);
        JPAKEParticipant jPAKEParticipant2 = new JPAKEParticipant("bob", "password".toCharArray(), jPAKEPrimeOrderGroup, sHA256Digest, secureRandom);
        JPAKERound1Payload createRound1PayloadToSend = jPAKEParticipant.createRound1PayloadToSend();
        JPAKERound1Payload createRound1PayloadToSend2 = jPAKEParticipant2.createRound1PayloadToSend();
        System.out.println("************ Round 1 **************");
        System.out.println("Alice sends to Bob: ");
        PrintStream printStream7 = System.out;
        printStream7.println("g^{x1}=" + createRound1PayloadToSend.getGx1().toString(16));
        PrintStream printStream8 = System.out;
        printStream8.println("g^{x2}=" + createRound1PayloadToSend.getGx2().toString(16));
        PrintStream printStream9 = System.out;
        printStream9.println("KP{x1}={" + createRound1PayloadToSend.getKnowledgeProofForX1()[0].toString(16) + "};{" + createRound1PayloadToSend.getKnowledgeProofForX1()[1].toString(16) + "}");
        PrintStream printStream10 = System.out;
        printStream10.println("KP{x2}={" + createRound1PayloadToSend.getKnowledgeProofForX2()[0].toString(16) + "};{" + createRound1PayloadToSend.getKnowledgeProofForX2()[1].toString(16) + "}");
        System.out.println("");
        System.out.println("Bob sends to Alice: ");
        PrintStream printStream11 = System.out;
        printStream11.println("g^{x3}=" + createRound1PayloadToSend2.getGx1().toString(16));
        PrintStream printStream12 = System.out;
        printStream12.println("g^{x4}=" + createRound1PayloadToSend2.getGx2().toString(16));
        PrintStream printStream13 = System.out;
        printStream13.println("KP{x3}={" + createRound1PayloadToSend2.getKnowledgeProofForX1()[0].toString(16) + "};{" + createRound1PayloadToSend2.getKnowledgeProofForX1()[1].toString(16) + "}");
        PrintStream printStream14 = System.out;
        printStream14.println("KP{x4}={" + createRound1PayloadToSend2.getKnowledgeProofForX2()[0].toString(16) + "};{" + createRound1PayloadToSend2.getKnowledgeProofForX2()[1].toString(16) + "}");
        System.out.println("");
        jPAKEParticipant.validateRound1PayloadReceived(createRound1PayloadToSend2);
        System.out.println("Alice checks g^{x4}!=1: OK");
        System.out.println("Alice checks KP{x3}: OK");
        System.out.println("Alice checks KP{x4}: OK");
        System.out.println("");
        jPAKEParticipant2.validateRound1PayloadReceived(createRound1PayloadToSend);
        System.out.println("Bob checks g^{x2}!=1: OK");
        System.out.println("Bob checks KP{x1},: OK");
        System.out.println("Bob checks KP{x2},: OK");
        System.out.println("");
        JPAKERound2Payload createRound2PayloadToSend = jPAKEParticipant.createRound2PayloadToSend();
        JPAKERound2Payload createRound2PayloadToSend2 = jPAKEParticipant2.createRound2PayloadToSend();
        System.out.println("************ Round 2 **************");
        System.out.println("Alice sends to Bob: ");
        PrintStream printStream15 = System.out;
        printStream15.println("A=" + createRound2PayloadToSend.getA().toString(16));
        PrintStream printStream16 = System.out;
        printStream16.println("KP{x2*s}={" + createRound2PayloadToSend.getKnowledgeProofForX2s()[0].toString(16) + "},{" + createRound2PayloadToSend.getKnowledgeProofForX2s()[1].toString(16) + "}");
        System.out.println("");
        System.out.println("Bob sends to Alice");
        PrintStream printStream17 = System.out;
        printStream17.println("B=" + createRound2PayloadToSend2.getA().toString(16));
        PrintStream printStream18 = System.out;
        printStream18.println("KP{x4*s}={" + createRound2PayloadToSend2.getKnowledgeProofForX2s()[0].toString(16) + "},{" + createRound2PayloadToSend2.getKnowledgeProofForX2s()[1].toString(16) + "}");
        System.out.println("");
        jPAKEParticipant.validateRound2PayloadReceived(createRound2PayloadToSend2);
        System.out.println("Alice checks KP{x4*s}: OK\n");
        jPAKEParticipant2.validateRound2PayloadReceived(createRound2PayloadToSend);
        System.out.println("Bob checks KP{x2*s}: OK\n");
        BigInteger calculateKeyingMaterial = jPAKEParticipant.calculateKeyingMaterial();
        BigInteger calculateKeyingMaterial2 = jPAKEParticipant2.calculateKeyingMaterial();
        System.out.println("********* After round 2 ***********");
        PrintStream printStream19 = System.out;
        printStream19.println("Alice computes key material \t K=" + calculateKeyingMaterial.toString(16));
        PrintStream printStream20 = System.out;
        printStream20.println("Bob computes key material \t K=" + calculateKeyingMaterial2.toString(16));
        System.out.println();
        deriveSessionKey(calculateKeyingMaterial);
        deriveSessionKey(calculateKeyingMaterial2);
        JPAKERound3Payload createRound3PayloadToSend = jPAKEParticipant.createRound3PayloadToSend(calculateKeyingMaterial);
        JPAKERound3Payload createRound3PayloadToSend2 = jPAKEParticipant2.createRound3PayloadToSend(calculateKeyingMaterial2);
        System.out.println("************ Round 3 **************");
        System.out.println("Alice sends to Bob: ");
        PrintStream printStream21 = System.out;
        printStream21.println("MacTag=" + createRound3PayloadToSend.getMacTag().toString(16));
        System.out.println("");
        System.out.println("Bob sends to Alice: ");
        PrintStream printStream22 = System.out;
        printStream22.println("MacTag=" + createRound3PayloadToSend2.getMacTag().toString(16));
        System.out.println("");
        jPAKEParticipant.validateRound3PayloadReceived(createRound3PayloadToSend2, calculateKeyingMaterial);
        System.out.println("Alice checks MacTag: OK\n");
        jPAKEParticipant2.validateRound3PayloadReceived(createRound3PayloadToSend, calculateKeyingMaterial2);
        System.out.println("Bob checks MacTag: OK\n");
        System.out.println();
        System.out.println("MacTags validated, therefore the keying material matches.");
    }
}
