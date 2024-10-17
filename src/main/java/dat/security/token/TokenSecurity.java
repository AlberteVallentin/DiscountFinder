package dat.security.token;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dat.security.enums.RoleType;

import java.text.ParseException;
import java.util.Date;

/**
 * Purpose: To provide JWT-related operations
 * Author: Thomas Hartmann
 * This class implements the ITokenSecurity interface to provide JWT-related operations.
 */
public class TokenSecurity implements ITokenSecurity {

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDTO getUserWithRoleFromToken(String token) throws ParseException {
        // Return a user with a single role from the JWT token
        SignedJWT jwt = SignedJWT.parse(token);
        String role = jwt.getJWTClaimsSet().getClaim("role").toString();
        String email = jwt.getJWTClaimsSet().getClaim("email").toString();

        RoleType userRoleType = RoleType.valueOf(role); // Convert role string to enum
        return new UserDTO(email, userRoleType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tokenIsValid(String token, String secret) throws ParseException, TokenVerificationException {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            return jwt.verify(new MACVerifier(secret));
        } catch (JOSEException e) {
            throw new TokenVerificationException("Could not verify token", e.getCause());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tokenNotExpired(String token) throws ParseException {
        return timeToExpire(token) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int timeToExpire(String token) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(token);
        return (int) (jwt.getJWTClaimsSet().getExpirationTime().getTime() - new Date().getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createToken(UserDTO user, String ISSUER, String TOKEN_EXPIRE_TIME, String SECRET_KEY) throws TokenCreationException {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())  // Use email as the subject
                .issuer(ISSUER)
                .claim("email", user.getEmail())
                .claim("role", user.getRoleType().toString()) // Store the role as a string in the token
                .expirationTime(new Date(new Date().getTime() + Integer.parseInt(TOKEN_EXPIRE_TIME)))
                .build();
            Payload payload = new Payload(claimsSet.toJSONObject());

            JWSSigner signer = new MACSigner(SECRET_KEY);
            JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);
            JWSObject jwsObject = new JWSObject(jwsHeader, payload);
            jwsObject.sign(signer);
            return jwsObject.serialize();

        } catch (JOSEException e) {
            e.printStackTrace();
            throw new TokenCreationException("Could not create token", e);
        }
    }
}
