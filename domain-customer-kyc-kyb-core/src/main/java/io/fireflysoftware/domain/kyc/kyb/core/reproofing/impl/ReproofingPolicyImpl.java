package io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ReproofingPolicyImpl {
    private final String issuer, realm, tenant, provider, policy, origin;
    private final Clock clock;

    public ReproofingPolicyImpl(String issuer, String realm, String tenant, String provider, String policy, String origin, Clock clock) {
        https(issuer); https(origin);
        URI u = URI.create(origin);
        if (!u.getPath().isEmpty() && !u.getPath().equals("/")) throw new IllegalArgumentException("Exact verification origin required");
        this.issuer=issuer; this.realm=text(realm); this.tenant=text(tenant); this.provider=text(provider);
        this.policy=text(policy); this.origin=origin.endsWith("/")?origin.substring(0,origin.length()-1):origin; this.clock=clock;
    }
    public void binding(ReproofingBinding b) {
        if (b==null || !issuer.equals(b.issuer()) || !realm.equals(b.realm()) || !tenant.equals(b.tenant())
                || !"HUMAN".equals(b.population()) || !"CREDENTIAL_REPLACEMENT".equals(b.purpose()) || b.partyId()==null) throw denied();
        text(b.subject()); text(b.generation()); text(b.credentialRevision()); text(b.nonce());
        if (b.nonce().length()>128) throw denied();
    }
    public void preparation(ReproofingBinding b, ReproofingAdmission a) {
        if (a==null || !b.equals(a.binding()) || !live(a.expiresAt())) throw denied();
    }
    public void admission(ReproofingBinding b, ReproofingAdmission a) {
        preparation(b,a);
        if (!a.eligible()) throw denied();
    }
    public void reproofingCase(ReproofingBinding b, ReproofingCaseResponse c) {
        if (c==null || c.caseId()==null || c.providerStartId()==null || !b.equals(c.binding())
                || !provider.equals(c.providerId()) || !policy.equals(c.policyId()) || !live(c.expiresAt())) throw denied();
    }
    public void evidence(ReproofingEvidenceRequest r, ReproofingCaseResponse c, VerifiedReproofingEvidence e) {
        reproofingCase(r.binding(),c);
        if (!c.caseId().equals(r.caseId()) || !provider.equals(r.providerId()) || !policy.equals(r.policyId())
                || !Objects.equals(c.providerSessionId(),r.providerSessionId()) || r.providerSessionId()==null
                || e==null || !r.caseId().equals(e.caseId()) || !r.binding().equals(e.binding())
                || !provider.equals(e.providerId()) || !policy.equals(e.policyId()) || !r.providerSessionId().equals(e.providerSessionId())
                || !r.binding().partyId().equals(e.referenceId()) || !e.documentAuthentic() || !e.liveMatch() || !e.fraudCleared() || !e.referenceMatched()
                || e.verifiedAt()==null || e.verifiedAt().isAfter(clock.instant()) || e.verifiedAt().isBefore(clock.instant().minusSeconds(600))
                || !live(e.expiresAt()) || e.expiresAt().isAfter(c.expiresAt())) throw denied();
        text(e.evidenceId()); text(e.evidenceDigest());
    }
    public String journeyUrl(String value) {
        if(value==null) return null;
        https(value); URI u=URI.create(value);
        String actual=u.getScheme()+"://"+u.getRawAuthority();
        if(!origin.equals(actual) || value.length()>2048) throw denied();
        return value;
    }
    public boolean live(Instant expiresAt) { return expiresAt!=null && expiresAt.isAfter(clock.instant()); }
    public static String text(String s) { if(s==null || s.isBlank() || s.length()>512 || s.chars().anyMatch(Character::isISOControl)) throw denied(); return s; }
    public static void https(String value) {
        try { URI u=URI.create(value); if(!"https".equals(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null || u.getFragment()!=null) throw denied(); }
        catch(IllegalArgumentException e) { throw denied(); }
    }
    public static ResponseStatusException denied() { return new ResponseStatusException(HttpStatus.FORBIDDEN,"Recovery refused"); }
    public static ResponseStatusException unavailable() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Recovery unavailable"); }
}
