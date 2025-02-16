package me.antidotaleks.iter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Teaming implements Listener {

    private static final HashMap<Player, ArrayList<Player>> teams = new HashMap<>();
    private static final ArrayList<Invite> invites = new ArrayList<>();



    // Public methods

    public static void invite(Player inviter, Player invitee) {
        invites.addFirst(new Invite(inviter, invitee, false));

        inviter.sendMessage("Invite sent to " + invitee.getName());
        invitee.sendMessage("You have been invited to join " + inviter.getName() + "'s team. ");

        answerText(inviter, invitee);
    }

    public static void requestToJoin(Player requester, Player requested) {
        invites.add(new Invite(requester, requested, true));

        requester.sendMessage("Request sent to " + requested.getName());
        requested.sendMessage(requester.getName() + " has requested to join your team. ");

        answerText(requester, requested);
    }

    public static void leave(Player player) {
        // Host leaves
        if (teams.containsKey(player))
            hostLeave(player, true);

        else // Team member leaves
            memberLeave(player, true);
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
            player.sendMessage("Team members: " + Arrays.toString(teams.get(player).toArray()));
        }
        // If team member
        else {
            Player teamLeader = findPlayerTeam(player);
            player.sendMessage("Team leader: " + teamLeader.getName());
            player.sendMessage("Team members: " + Arrays.toString(teams.get(teamLeader).toArray()));
        }
    }

    public static void accept(Player player, @Nullable Player accepted) {
        // If accepted == null, accepts the latest invite
        for (Invite invite : invites) {
            if (invite.invitee == player && (accepted == null || invite.inviter == accepted)) {
                if (invite.request)
                    joinPlayerToTeam(invite.inviter, invite.invitee);
                else
                    joinPlayerToTeam(invite.invitee, invite.inviter);
            }
        }
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        teams.put(event.getPlayer(), new ArrayList<>());
    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        // If host leaves, disband team
        if (teams.containsKey(event.getPlayer()))
            hostLeave(event.getPlayer(), false);

        else // If team member leaves, remove them from team
            memberLeave(event.getPlayer(), false);
    }



    // Utility methods

    private static void joinPlayerToTeam(Player host, Player newMember) {
        host.sendMessage(newMember.getName() + " has joined the team.");
        newMember.sendMessage("You have joined " + host.getName() + "'s team.");
        for (Player player : teams.get(host)) {
            player.sendMessage(newMember.getName() + " has joined the team.");
        }
        teams.get(host).add(newMember);
        teams.remove(newMember);

    }

    private static void hostLeave(Player host, boolean hostStillOnline) {
        for (Player player : teams.get(host)) {
            player.sendMessage("Host "+ host.getName() + " has left the team. Disbanding team.");
            teams.put(player, new ArrayList<>());
        }
        if (hostStillOnline) teams.put(host, new ArrayList<>());
        else teams.remove(host);
    }

    private static void memberLeave(Player newMember, boolean memberStillOnline) {
        Player teamLeader = findPlayerTeam(newMember);

        if (teamLeader != null) {
            teams.get(teamLeader).remove(newMember);
            teamLeader.sendMessage(newMember.getName() + " has left the team.");
            for (Player player : teams.get(teamLeader)) {
                player.sendMessage(newMember.getName() + " has left the team.");
            }
        }
        if (memberStillOnline) teams.put(newMember, new ArrayList<>());
        else teams.remove(newMember);
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
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team accept "+sender.getName()));
        TextComponent decline = new TextComponent("[decline]");
        decline.setColor(ChatColor.RED);
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team decline "+sender.getName()));
        answerer.spigot().sendMessage(accept, decline);
    }

    // Invite await

    

    private static class Invite {
        public final Player inviter;
        public final Player invitee;
        public final long inviteTime;
        public final boolean request;


        public Invite(Player inviter, Player invitee, boolean requestToJoin) {
            this.inviter = inviter;
            this.invitee = invitee;
            this.inviteTime = System.currentTimeMillis();
            this.request = requestToJoin;
        }
    }

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
}
