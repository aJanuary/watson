package com.ajanuary.watson.membership;

import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
import com.ajanuary.watson.portalapi.PortalApiClient;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MembersApiClient {

  private final URI membersApiUrl;
  private final PortalApiClient portalApiClient;

  public MembersApiClient(URI membersApiUrl, PortalApiClient portalApiClient) {
    this.membersApiUrl = membersApiUrl;
    this.portalApiClient = portalApiClient;
  }

  public Map<String, MembershipStatus> getMemberStatus(Collection<DiscordUser> discordUsers)
      throws IOException, InterruptedException {

    var postData = new HashMap<String, Object>();
    postData.put(
        "discordUsers",
        discordUsers.stream()
            .map(
                discordUser -> {
                  var discordUserMap = new HashMap<String, String>();
                  discordUserMap.put("id", discordUser.userId());
                  discordUserMap.put("username", discordUser.username());
                  return discordUserMap;
                })
            .toList());

    var memberships = new HashMap<String, MembershipStatus>();
    var responseData = portalApiClient.send(membersApiUrl, postData);
    responseData
        .fields()
        .forEachRemaining(
            entry -> {
              if (entry.getValue().isTextual()) {
                memberships.put(
                    entry.getKey(), MembershipStatus.verification(entry.getValue().asText()));
              } else {
                var name = entry.getValue().get("name").asText();
                var roles = new ArrayList<String>();
                entry
                    .getValue()
                    .get("roles")
                    .elements()
                    .forEachRemaining(
                        role -> {
                          roles.add(role.asText());
                        });
                memberships.put(
                    entry.getKey(), MembershipStatus.member(new MemberDetails(name, roles)));
              }
            });
    return memberships;
  }

  public static class MembershipStatus {
    private final MemberDetails details;
    private final String verificationUrl;

    private MembershipStatus(MemberDetails details, String verificationUrl) {
      this.details = details;
      this.verificationUrl = verificationUrl;
    }

    public static MembershipStatus member(MemberDetails details) {
      return new MembershipStatus(details, null);
    }

    public static MembershipStatus verification(String verificationUrl) {
      return new MembershipStatus(null, verificationUrl);
    }

    public void map(Consumer<MemberDetails> ifMember, Consumer<String> ifVerification) {
      if (details != null) {
        ifMember.accept(details);
      } else {
        ifVerification.accept(verificationUrl);
      }
    }
  }

  public record MemberDetails(String name, List<String> roles) {}
}
