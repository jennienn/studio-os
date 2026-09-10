package com.studioos.auth;
import com.studioos.common.ApiException;
import com.studioos.security.OperatorPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class OAuthAccountService {
    private final UserRepository users;
    private final AuthAccountRepository accounts;
    private final TransactionTemplate transactions;
    public OAuthAccountService(UserRepository users,AuthAccountRepository accounts,PlatformTransactionManager manager) {
        this.users=users; this.accounts=accounts; transactions=new TransactionTemplate(manager);
    }
    public OperatorPrincipal resolve(OAuthIdentityMapper.Identity identity) {
        try { return transactions.execute(status -> {
            var existing=accounts.findByProviderAndProviderUserId(identity.provider(),identity.providerUserId());
            if (existing.isPresent()) return principal(users.findById(existing.get().userId).orElseThrow());
            OAuthIdentityMapper.requireName(identity);
            var user=users.saveAndFlush(new OperatorUser(identity.email(),identity.name(),"ACTIVE"));
            accounts.saveAndFlush(new AuthAccount(user.id,identity.provider(),identity.providerUserId(),null));
            return principal(user);
        }); } catch (DataIntegrityViolationException race) {
            // The losing transaction rolls back its new User as well as AuthAccount.
            return transactions.execute(status -> accounts.findByProviderAndProviderUserId(identity.provider(),identity.providerUserId())
                .map(a -> principal(users.findById(a.userId).orElseThrow())).orElseThrow(() -> race));
        }
    }
    private OperatorPrincipal principal(OperatorUser user) {
        if (!user.status.equals("ACTIVE")) throw new ApiException(403,"ACCOUNT_DISABLED","로그인할 수 없습니다.");
        return new OperatorPrincipal(user.id,user.securityVersion);
    }
}
