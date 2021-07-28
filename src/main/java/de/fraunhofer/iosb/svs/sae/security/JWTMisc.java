package de.fraunhofer.iosb.svs.sae.security;

import java.util.Date;
import java.util.List;

import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JWTMisc {
	private static String jwtSecret;
	private static String jwtIssuer;
	
	@Value("${jwt.secret}")
	public void setSecret(String secret) {
		JWTMisc.jwtSecret = secret;
	}

	@Value("${jwt.issuer}")
	public void setIssuer(String issuer) {
		JWTMisc.jwtIssuer = issuer;
	}

	public static DecodedJWT verifyAndDecode(String token) {
		DecodedJWT jwt = null;
		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
			JWTVerifier verifier = JWT.require(algorithm).withIssuer("analysisEngine").build();
			jwt = verifier.verify(token);
		} catch (JWTVerificationException exception) {}
		
		return jwt;
	}

	public static String signAndGenerate(List<Pair<String,String>> claims) {
		Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
		Builder token = JWT.create().withIssuer(jwtIssuer).withIssuedAt(new Date());
	
		claims.forEach(claim->{
			token.withClaim(claim.getValue0(), claim.getValue1());
		});
	
		return token.sign(algorithm);
	}
}
