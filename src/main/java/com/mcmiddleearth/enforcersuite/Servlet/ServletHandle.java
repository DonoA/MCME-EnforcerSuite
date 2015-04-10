/*
 * This file is part of BoundHelper.
 * 
 * BoundHelper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * BoundHelper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with BoundHelper.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package com.mcmiddleearth.enforcersuite.Servlet;

import com.mcmiddleearth.enforcersuite.DBmanager.DBmanager;
import com.mcmiddleearth.enforcersuite.EnforcerSuite;
import com.mcmiddleearth.enforcersuite.Records.Infraction;
import com.mcmiddleearth.enforcersuite.Utils.LogUtil;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
/**
 *
 * @author Donovan
 */
public class ServletHandle extends AbstractHandler{
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String[] args = target.split("/");
        response.setHeader("Server", EnforcerSuite.getPrefix());
        if(args.length>=2){
            if(args[1].equalsIgnoreCase("current")){
                if(args[2].equalsIgnoreCase("ob")){
                    baseRequest.setHandled(true);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().print(ServletDBmanager.getOBs(true));
                }else if(args[2].equalsIgnoreCase("ban")){
                    baseRequest.setHandled(true);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().print(ServletDBmanager.getBans(true));
                }
            }else if(args[1].equalsIgnoreCase("archive")){
                if(args[2].equalsIgnoreCase("ob")){
                    baseRequest.setHandled(true);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().print(ServletDBmanager.getOBs(false));
                }else if(args[2].equalsIgnoreCase("ban")){
                    baseRequest.setHandled(true);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().print(ServletDBmanager.getBans(false));
                }
            }else if(args[1].equalsIgnoreCase("files") && args.length>=2){
                UUID requestUUID;
                try{
                    requestUUID = UUID.fromString(args[2]);
                }catch (Exception e){
                    requestUUID = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
                }
                baseRequest.setHandled(true);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Current:");
                response.getWriter().print(EnforcerSuite.getJSonParser().writeValueAsString(ServletDBmanager.getRecord(requestUUID).getCurrentInfraction()) + "\n");
                response.getWriter().println("Archived:");
                response.getWriter().print(EnforcerSuite.getJSonParser().writeValueAsString(ServletDBmanager.getRecord(requestUUID).getOldInfractions()) + "\n");
            }else{
                baseRequest.setHandled(true);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Use one of the following" + "\n" + "/<current|archive>/<ob|ban>" + "\n" + "/files/<player (uuid or name) >");
            }
        }else{
            baseRequest.setHandled(true);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Use one of the following" + "\n" + "/<current|archive>/<ob|ban>" + "\n" + "/files/<player (uuid or name) >");
        }
    }
    
    public static class TCPconnect extends Thread {
        
        @Override
        public void run() {
            String req;
            ServerSocket welcomeSocket = null;
            try {
                welcomeSocket = new ServerSocket(6789);
                List<String> rtn = new ArrayList<>();
                ServletDBmanager.loadIncomplete();
                while(true){
                    try (Socket connectionSocket = welcomeSocket.accept()) {
                        BufferedReader inFromClient =
                                new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                        req = TCPkeyHandle.getRequest(inFromClient.readLine());
                        if(req.equalsIgnoreCase("ping")){
                            rtn.clear();
                            LogUtil.printDebug(ServletDBmanager.Incomplete);
                            for(Infraction inf : ServletDBmanager.Incomplete){
                                if(inf.getOBname() == null){
                                    if(inf.isBan()){
                                        rtn.add(inf.getOBuuid().toString() + " - Ban");
                                    }else{
                                        rtn.add(inf.getOBuuid().toString() + " - OathBreaker");
                                    }
                                }else{
                                    if(inf.isBan()){
                                        rtn.add(inf.getOBuuid().toString() + " - " + inf.getOBname() + " - " + "Ban");
                                    }else{
                                        rtn.add(inf.getOBuuid().toString() + " - " + inf.getOBname() + " - " + "OathBreaker");
                                    }
                                }
                            }
                            LogUtil.printDebug("Successful Ping");
                            outToClient.writeBytes(EnforcerSuite.getJSonParser().writeValueAsString(rtn));
                        }else if(req.contains("fetch")){
                            if(req.contains("archive")){
                                HashMap<String, HashMap<String, ArrayList<String>>> toSend = new HashMap<String, HashMap<String, ArrayList<String>>>();
                                HashMap<String, ArrayList<String>> bag =  new HashMap<String, ArrayList<String>>();
                                //obs
                                bag.put("Current", new ArrayList<String>(ServletDBmanager.getOBs(true)));
                                List<String> hold = ServletDBmanager.getOBs(false);
                                hold.removeAll(ServletDBmanager.getOBs(true));
                                bag.put("Archived", new ArrayList<String>(hold));
                                toSend.put("OBs", bag);
                                //bans
                                bag =  new HashMap<String, ArrayList<String>>();
                                bag.put("Current", new ArrayList<String>(ServletDBmanager.getBans(true)));
                                hold = ServletDBmanager.getBans(false);
                                hold.removeAll(ServletDBmanager.getBans(true));
                                bag.put("Archived", new ArrayList<String>(hold));
                                toSend.put("Bans", bag);
                                //send
                                outToClient.writeBytes(EnforcerSuite.getJSonParser().writeValueAsString(toSend));
                            }else if(req.contains("record")){
                                LogUtil.printDebug(req);
                                int start = req.indexOf("$");
                                String uuid = req.substring(start + 1, req.indexOf(" ", start));
                                LogUtil.printDebug(uuid);
                                outToClient.writeBytes(EnforcerSuite.getJSonParser().writeValueAsString(ServletDBmanager.getRecord(UUID.fromString(uuid)).getOldInfractions()));
                            }else{
                                for(Infraction inf : ServletDBmanager.Incomplete){
                                    if(req.contains(inf.getOBuuid().toString())){
                                        outToClient.writeBytes(EnforcerSuite.getJSonParser().writeValueAsString(inf));
                                    }
                                }
                            }
                        }else if(req.contains("return")){ //only works with OBs in the current folder D:
                            LogUtil.printDebug(req);
                            req = req.substring(req.indexOf("$")+1);
                            ReturnClass rtnclss = EnforcerSuite.getJSonParser().readValue(req, ReturnClass.class);
                            Infraction[] infs = new Infraction[ServletDBmanager.Incomplete.size()];
                            ServletDBmanager.Incomplete.toArray(infs);
                            Infraction inf = null;
                            for(Infraction i : infs){
                                if(req.contains(i.getOBuuid().toString())){
                                    inf = i;
                                    if(rtnclss.isDone()){
                                        ServletDBmanager.Incomplete.remove(i);
                                    }
                                }
                            }
                            if(inf == null)
                                return;
                            //bans
                            if(rtnclss.getBannedOn().isBuild()){
                                inf.getBannedOn().add("build");
                                if(rtnclss.getBannedOn().isEauto()){
                                    //ban them
                                }
                            }
                            if(rtnclss.getBannedOn().isFreebuild()){
                                inf.getBannedOn().add("freebuild");
                                if(rtnclss.getBannedOn().isEauto()){
                                    //ban them
                                }
                            }
                            if(rtnclss.getBannedOn().isTeamspeak()){
                                inf.getBannedOn().add("teamspeak");
                                if(rtnclss.getBannedOn().isEauto()){
                                    //ban them
                                }
                            }
                            if(rtnclss.getBannedOn().isForums()){
                                inf.getBannedOn().add("forums");
                            }
                            if(rtnclss.getBannedOn().isOthercheck()){
                                inf.getBannedOn().add(rtnclss.getBannedOn().getOthertxt());
                            }
                            //reasons
                            if(rtnclss.getReasons().isBlocks()){
                                inf.getReasons().add("Unauthorised Block Break/Place");
                                if(rtnclss.getReasons().isEauto()){
                                    //Collect Evidence
                                }
                            }
                            if(rtnclss.getReasons().isSpam()){
                                inf.getReasons().add("Spamming Chat");
                                if(rtnclss.getReasons().isEauto()){
                                    //Collect Evidence
                                }
                            }
                            if(rtnclss.getReasons().isSocial()){
                                inf.getReasons().add("Social Indiscretion (Inappropriate Username, Profane/Derogatory Language, Political/Religious Discussions, Referencing Vulgar/Explicit Material)");
                            }
                            if(rtnclss.getReasons().isIgnoring()){
                                inf.getReasons().add("Ignoring Staff Direction / Impeding Work");
                            }
                            if(rtnclss.getReasons().isImpersonating()){
                                inf.getReasons().add("Impersonating Staff Member");
                            }
                            if(rtnclss.getReasons().isMods()){
                                inf.getReasons().add("Use of Detrimental 3rd Party Mods");
                            }
                            if(rtnclss.getReasons().isAds()){
                                inf.getReasons().add("Advertising");
                                if(rtnclss.getReasons().isEauto()){
                                    //Collect Evidence
                                }
                            }
                            if(rtnclss.getReasons().isObassist()){
                                inf.getReasons().add("Assisting Oathbreakers");
                                if(rtnclss.getReasons().isEauto()){
                                    //Collect Evidence
                                }
                            }
                            if(rtnclss.getReasons().isTeamspeak()){
                                inf.getReasons().add("TeamSpeak (Infractions)");
                            }
                            if(rtnclss.getReasons().isAlt()){
                                inf.getReasons().add("Alt Account");
                            }
                            if(rtnclss.getReasons().isOthercheck()){
                                inf.getReasons().add(rtnclss.getReasons().getOthertxt());
                            }
                            inf.getEvidence().add(rtnclss.getEvidence());
                            inf.setNotes(rtnclss.getNotes());
                            DBmanager.OBs.put(UUID.fromString(rtnclss.getObuuid()), inf);
                            DBmanager.saveOB(UUID.fromString(rtnclss.getObuuid()));
                            if(!Bukkit.getOfflinePlayer(UUID.fromString(rtnclss.getObuuid())).isOnline()){
                                DBmanager.OBs.remove(UUID.fromString(rtnclss.getObuuid()));
                            }
                        }else if(req.contains("request")){
                            req = req.substring(req.indexOf("$")+1);
                            RequestType request = EnforcerSuite.getJSonParser().readValue(req, RequestType.class);
                            if(request.getBase().equalsIgnoreCase("archive")){
                                DBmanager.archiveBan(UUID.fromString(request.getArgs()[0]));
                            }
                        }
                            
                    } catch (IOException ex) {
                        LogUtil.printErr("Failed to Decode from forum");
                        LogUtil.printDebugStack(ex);
                    }
                }
            } catch (IOException ex) {
                LogUtil.printErr("Failed to Decode from forum");
                LogUtil.printDebugStack(ex);
            }
            
            
        }
    }
    
    public class RequestType{
        @Getter @Setter
        private String base;
        @Getter @Setter
        private String[] args;
        
        public RequestType(){}
        
    }
}
