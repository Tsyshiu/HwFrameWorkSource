package com.android.server.accounts;

import android.accounts.Account;
import android.util.LruCache;
import android.util.Pair;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/* access modifiers changed from: package-private */
public class TokenCache {
    private static final int MAX_CACHE_CHARS = 64000;
    private TokenLruCache mCachedTokens = new TokenLruCache();

    TokenCache() {
    }

    private static class Value {
        public final long expiryEpochMillis;
        public final String token;

        public Value(String token2, long expiryEpochMillis2) {
            this.token = token2;
            this.expiryEpochMillis = expiryEpochMillis2;
        }
    }

    private static class Key {
        public final Account account;
        public final String packageName;
        public final byte[] sigDigest;
        public final String tokenType;

        public Key(Account account2, String tokenType2, String packageName2, byte[] sigDigest2) {
            this.account = account2;
            this.tokenType = tokenType2;
            this.packageName = packageName2;
            this.sigDigest = sigDigest2;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof Key)) {
                return false;
            }
            Key cacheKey = (Key) o;
            if (!Objects.equals(this.account, cacheKey.account) || !Objects.equals(this.packageName, cacheKey.packageName) || !Objects.equals(this.tokenType, cacheKey.tokenType) || !Arrays.equals(this.sigDigest, cacheKey.sigDigest)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return ((this.account.hashCode() ^ this.packageName.hashCode()) ^ this.tokenType.hashCode()) ^ Arrays.hashCode(this.sigDigest);
        }
    }

    private static class TokenLruCache extends LruCache<Key, Value> {
        private HashMap<Account, Evictor> mAccountEvictors = new HashMap<>();
        private HashMap<Pair<String, String>, Evictor> mTokenEvictors = new HashMap<>();

        private class Evictor {
            private final List<Key> mKeys = new ArrayList();

            public Evictor() {
            }

            public void add(Key k) {
                this.mKeys.add(k);
            }

            public void evict() {
                for (Key k : this.mKeys) {
                    TokenLruCache.this.remove(k);
                }
            }
        }

        public TokenLruCache() {
            super(TokenCache.MAX_CACHE_CHARS);
        }

        /* access modifiers changed from: protected */
        public int sizeOf(Key k, Value v) {
            return v.token.length();
        }

        /* access modifiers changed from: protected */
        public void entryRemoved(boolean evicted, Key k, Value oldVal, Value newVal) {
            Evictor evictor;
            if (oldVal != null && newVal == null && (evictor = this.mTokenEvictors.remove(new Pair(k.account.type, oldVal.token))) != null) {
                evictor.evict();
            }
        }

        public void putToken(Key k, Value v) {
            Pair<String, String> mapKey = new Pair<>(k.account.type, v.token);
            Evictor tokenEvictor = this.mTokenEvictors.get(mapKey);
            if (tokenEvictor == null) {
                tokenEvictor = new Evictor();
            }
            tokenEvictor.add(k);
            this.mTokenEvictors.put(mapKey, tokenEvictor);
            Evictor accountEvictor = this.mAccountEvictors.get(k.account);
            if (accountEvictor == null) {
                accountEvictor = new Evictor();
            }
            accountEvictor.add(k);
            this.mAccountEvictors.put(k.account, tokenEvictor);
            put(k, v);
        }

        public void evict(String accountType, String token) {
            Evictor evictor = this.mTokenEvictors.get(new Pair(accountType, token));
            if (evictor != null) {
                evictor.evict();
            }
        }

        public void evict(Account account) {
            Evictor evictor = this.mAccountEvictors.get(account);
            if (evictor != null) {
                evictor.evict();
            }
        }
    }

    public void put(Account account, String token, String tokenType, String packageName, byte[] sigDigest, long expiryMillis) {
        Preconditions.checkNotNull(account);
        if (token != null && System.currentTimeMillis() <= expiryMillis) {
            this.mCachedTokens.putToken(new Key(account, tokenType, packageName, sigDigest), new Value(token, expiryMillis));
        }
    }

    public void remove(String accountType, String token) {
        this.mCachedTokens.evict(accountType, token);
    }

    public void remove(Account account) {
        this.mCachedTokens.evict(account);
    }

    public String get(Account account, String tokenType, String packageName, byte[] sigDigest) {
        Value v = (Value) this.mCachedTokens.get(new Key(account, tokenType, packageName, sigDigest));
        long currentTime = System.currentTimeMillis();
        if (v != null && currentTime < v.expiryEpochMillis) {
            return v.token;
        }
        if (v == null) {
            return null;
        }
        remove(account.type, v.token);
        return null;
    }
}
