package me.antidotaleks.iter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public final class Teaming implements Listener {

    private static final HashMap<Player, ArrayList<Player>> teams = new HashMap<>();
    private static final ArrayList<Invite> invites = new ArrayList<>();



    // Public methods

    public static void invite(Player inviter, Player invitee) {
        // Remove previous invites to the invitee and remove requests from the invitee
        for (int i = invites.size()-1; i >= 0; i--) {
            Invite invite = invites.get(i);
            if (invite.invitee == invitee || invite.inviter == invitee)
                invites.remove(i);
        }

        if(teams.containsKey(inviter) && teams.get(inviter).contains(invitee)) {
            inviter.sendMessage("Player is already in your team");
            return;
        }
        invites.addFirst(new Invite(inviter, invitee, false));

        inviter.sendMessage("Invite sent to " + invitee.getName());
        invitee.sendMessage("You have been invited to join " + inviter.getName() + "'s team. ");

        answerText(inviter, invitee);
    }

    public static void requestToJoin(Player requester, Player requested) {
        // Remove previous requests to join any player and remove invites to the requested player
        for (int i = invites.size()-1; i >= 0; i--) {
            Invite invite = invites.get(i);
            if (invite.invitee == requester || invite.inviter == requester || invite.invitee == requested)
                invites.remove(i);
        }

        // If requester is already in the team
        if (teams.containsKey(requested) && teams.get(requested).contains(requester)) {
            requester.sendMessage("You are already in "+requested.getName()+"'s team");
            return;
        }

        // Create invite
        invites.add(new Invite(requester, requested, true));

        requester.sendMessage("Request sent to " + requested.getName());
        requested.sendMessage(requester.getName() + " has requested to join your team. ");

        answerText(requester, requested);
    }

    public static void leave(Player player) {
        leaveTeam(player);
    }

    public static void kick(Player kicker, Player kicked) {
        if (!teams.containsKey(kicker)) {
            kicker.sendMessage("You are not a host");
            return;
        }
        if (!teams.get(kicker).contains(kicked)) {
            kicker.sendMessage("Player not found in your team");
            return;
        }

        teams.get(kicker).remove(kicked);
        teams.put(kicked, new ArrayList<>());

        kicked.sendMessage("You have been kicked from the team.");
        kicker.sendMessage(kicked.getName() + " has been kicked from the team.");
        for (Player player : teams.get(kicker)) {
            player.sendMessage(kicked.getName() + " has been kicked from the team.");
        }
    }

    public static void list(Player player) {
        // If host
        if (teams.containsKey(player)) {
            player.sendMessage("Host: " + player.getName());
            player.sendMessage("Team members: " + teams.get(player).stream().map(Player::getName).toList());
        }
        // If team member
        else {
            Player teamLeader = findPlayerTeam(player);
            player.sendMessage("Team leader: " + teamLeader.getName());
            player.sendMessage("Team members: " + teams.get(teamLeader).stream().map(Player::getName).toList());
        }
    }

    public static void accept(Player player, @Nullable Player accepted) {
        // If accepted == null, accepts the latest invite
        for (Invite invite : invites) {
            if (invite.invitee != player || !(accepted == null || invite.inviter == accepted))
                continue;

            if (!invite.request)
                joinPlayerToTeam(invite.invitee, invite.inviter);
            else
                joinPlayerToTeam(invite.inviter, invite.invitee);

            invites.remove(invite);
            return;
        }
        player.sendMessage("Invite not found");
    }

    public static void decline(Player player, @Nullable Player declined) {
        // If declined == null, declines the latest invite
        for (Invite invite : invites) {
            if (invite.invitee != player || (declined != null && invite.inviter != declined))
                continue;

            invite.inviter.sendMessage(player.getName() + " has declined your invite.");
            player.sendMessage("You have declined " + invite.inviter.getName() + "'s invite.");

            invites.remove(invite);
            return;
        }
        player.sendMessage("Invite not found");
    }


    public static void onEnable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            teams.put(player, new ArrayList<>());
        }
        startRemovingOldInvites();
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        teams.put(event.getPlayer(), new ArrayList<>());
        SetupManager.hideNameTeam.addEntry(event.getPlayer().getName());
    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        // If host leaves, disband team
        leaveTeam(event.getPlayer());
    }

    public static boolean isHost(Player player) {
        return teams.containsKey(player);
    }

    public static ArrayList<Player> getTeam(Player player) {
        //noinspection unchecked
        return (ArrayList<Player>) teams.get(player).clone();
    }

    public static Player[] getTeamWithHost(Player player) {
        ArrayList<Player> team = getTeam(player);
        team.addFirst(player);
        return team.toArray(new Player[0]);
    }



    // Utility methods

    private static void joinPlayerToTeam(Player newMember, Player host) {
        leaveTeam(newMember);
        host.sendMessage(newMember.getName() + " has joined the team.");
        newMember.sendMessage("You have joined " + host.getName() + "'s team.");
        for (Player player : teams.get(host)) {
            player.sendMessage(newMember.getName() + " has joined the team.");
        }

        teams.get(host).add(newMember);
        teams.remove(newMember);

    }

    private static boolean isInTeam(Player player, Player teamLeader) {
        return teams.containsKey(teamLeader) && teams.get(teamLeader).contains(player);
    }

    private static void leaveTeam(Player player) {
        boolean isOnline = player.isOnline();
        if (teams.containsKey(player)) {
            if (!teams.get(player).isEmpty())
                hostLeave(player, isOnline);
        } else
            memberLeave(player, isOnline);
    }

    private static void hostLeave(Player host, boolean hostStillOnline) {
        for (Player player : teams.get(host)) {
            player.sendMessage("Host "+ host.getName() + " has left the team. Disbanding team.");
            teams.put(player, new ArrayList<>());
        }
        if (hostStillOnline) {
            host.sendMessage("Team disbanded");
            teams.put(host, new ArrayList<>());
        }
        else teams.remove(host);
    }

    private static void memberLeave(Player member, boolean memberStillOnline) {
        Player teamLeader = findPlayerTeam(member);

        if (teamLeader != null) {
            teams.get(teamLeader).remove(member);
            teamLeader.sendMessage(member.getName() + " has left the team.");
            for (Player player : teams.get(teamLeader)) {
                player.sendMessage(member.getName() + " has left the team.");
            }
        }
        if (memberStillOnline) {
            teams.put(member, new ArrayList<>());
            member.sendMessage("You have left the team");
        }
        else teams.remove(member);
    }

    private static Player findPlayerTeam(Player player) {
        for (Player teamLeader : teams.keySet()) {
            if (teams.get(teamLeader).contains(player)) {
                return teamLeader;
            }
        }
        return null;
    }

    private static void answerText(Player sender, Player answerer) {
        TextComponent accept = new TextComponent("[accept] ");
        accept.setColor(ChatColor.GREEN);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/iteam accept "+sender.getName()));
        TextComponent decline = new TextComponent("[decline]");
        decline.setColor(ChatColor.RED);
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/iteam decline "+sender.getName()));
        answerer.spigot().sendMessage(accept, decline);
    }

    // Invite


    // Remove old invites

    private static boolean startOnce = false;
    public static void startRemovingOldInvites() {
        if (startOnce)
            return;
        startOnce = true;

        new BukkitRunnable(){
            @Override
            public void run() {
                removeOldInvites();
            }
        }.run();
    }

    private static void removeOldInvites() {
        long currentTime = System.currentTimeMillis();

        for (int i = invites.size()-1; i >= 0; i--) {
            // Stop if invites list is empty
            if (invites.isEmpty()) {
                startRemoveTimer(0);
                break;
            }

            long delta = currentTime - invites.getLast().inviteTime;
            // Remove oldest if it's older than 30 seconds
            if (delta > 30_000)
                invites.removeLast();
            // Wait if oldest is less than 30 seconds
            else startRemoveTimer(delta);

        }

    }

    private static void startRemoveTimer(long delta) {
        new BukkitRunnable() {
            @Override
            public void run() {
                removeOldInvites();
            }
        }.runTaskLater(Iter.plugin, (31_000 - delta) * 20/1000);
    }

    private static class Invite {
        public final Player inviter;
        public final Player invitee;
        public final boolean request;
        public final long inviteTime;

        public Invite(Player inviter, Player invitee, boolean requestToJoin) {
            this.inviter = inviter;
            this.invitee = invitee;
            this.request = requestToJoin;
            this.inviteTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "Invite{" +
                    "inviter=" + inviter.getName() +
                    ", invitee=" + invitee.getName() +
                    ", request=" + request +
                    ", inviteTime=" + inviteTime +
                    '}';
        }
    }
}

