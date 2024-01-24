import be.goosegame.App;
import be.goosegame.DiceRollerService;
import be.goosegame.Player;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AppTester {


    private Request req = mock(Request.class);
    private Response res = mock(Response.class);

    private DiceRollerService drs = mock(DiceRollerService.class);

    private final App app = new App(drs);

    @Test
    public void testNoDuplicateNickName(){
        Player player = new Player("Koen", "kuun", 0);
        app.players.add(player);
        when(req.body()).thenReturn("{ \"name\": \"Koen\", \"nickname\": \"kuun\"}");

        String result = app.createPlayer(req, res);
        
        verify(res).status(400);
        Assertions.assertEquals(result, "{\"error\": \"nickname already taken: kuun\"}");

    }

    @Test
    public void testNoMoreThanFourPlayers(){
        for(int i = 0; i < 4; i++){
            Player player = new Player("Koen", "kuun" + i, 0);
            app.players.add(player);
        }
        when(req.body()).thenReturn("{ \"name\": \"Koen\", \"nickname\": \"kuun\"}");

        String result = app.createPlayer(req, res);

        verify(res).status(400);
        Assertions.assertTrue(result.startsWith("{\"error\": \"too many players already:"));
    }

    @Test
    public void testNoSinglePlayer(){
        Player player = new Player("Koen", "kuun", 0);
        app.players.add(player);

        String result = app.roll(req, res);
        
        verify(res).status(400);
        Assertions.assertEquals("{\"error\": \"Game not started, waiting for more players\"}", result);
    }

    @Test
    public void testNoPlayersAfterGameStart(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 0);
            app.players.add(player);
        }
        when(req.body()).thenReturn("{ \"name\": \"Koen\", \"nickname\": \"kuun\"}");
        app.setGameStarted(true);
        
        String result = app.createPlayer(req,res);
        
        verify(res).status(400);
        Assertions.assertTrue(result.startsWith("{\"error\": \"game has started already with: "));
    }

    @Test
    public void testNoPlayersAfterGameEnd(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 0);
            app.players.add(player);
        }
        when(req.body()).thenReturn("{ \"name\": \"Koen\", \"nickname\": \"kuun\"}");
        app.setGameOver(true);

        String result = app.createPlayer(req,res);
        
        verify(res).status(400);
        Assertions.assertTrue(result.startsWith("{\"error\": \"game has ended already with: "));
    }

    @Test
    public void testNoRollsAfterGameEnd(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 0);
            app.players.add(player);
        }
        when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());
        app.setGameOver(true);

        String result = app.roll(req,res);
        
        verify(res).status(400);
        Assertions.assertEquals("{\"error\": \"The game is over\"}", result);
    }

    @Test
    public void testNoRollByNonExistingPlayer(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 0);
            app.players.add(player);
        }
        when(req.params("id")).thenReturn(UUID.randomUUID().toString());

        String result = app.roll(req,res);
        
        verify(res).status(400);
        Assertions.assertEquals("{\"error\": \"User rolling dice was not in game!\"}", result);
    }

    @Test
    public void testNoRollWithoutUUID(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 0);
            app.players.add(player);
        }

        String result = app.roll(req,res);
        
        verify(res).status(400);
        Assertions.assertEquals("{\"error\": \"User rolling dice was not specified!\"}", result);
    }

    @Test
    public void testGooseSingle(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, i * 2);
            app.players.add(player);
        }
        //rolls 3
        when(drs.roll()).thenReturn(new JSONObject("{\"success\":true,\"dice\":[{\"value\":1,\"type\":\"d6\"},{\"value\":2,\"type\":\"d6\"}]}"));

        Arrays.asList(5,14,23,9,18,27).forEach(goose -> {
            app.players.get(0).setPosition(goose - 3);
            when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());
            
            app.roll(req,res);
            Assertions.assertEquals(goose + 3, app.players.get(0).getPosition());
            
            app.setNextPlayer(app.players.get(0));
        });
    }

    @Test
    public void testGooseMulti(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, i * 2);
            app.players.add(player);
        }
        //rolls 4
        when(drs.roll()).thenReturn(new JSONObject("{\"success\":true,\"dice\":[{\"value\":2,\"type\":\"d6\"},{\"value\":2,\"type\":\"d6\"}]}"));
        app.players.get(0).setPosition(1);
        when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());
        
        app.roll(req,res);
        
        Assertions.assertEquals(13, app.players.get(0).getPosition());
    }


    @Test
    public void testSwitch(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, i * 2);
            app.players.add(player);
        }
        //rolls 2
        when(drs.roll()).thenReturn(new JSONObject("{\"success\":true,\"dice\":[{\"value\":1,\"type\":\"d6\"},{\"value\":1,\"type\":\"d6\"}]}"));
        when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());

        Assertions.assertEquals(0, app.players.get(0).getPosition());
        Assertions.assertEquals(2, app.players.get(1).getPosition());
        
        app.roll(req,res);
        
        Assertions.assertEquals(2, app.players.get(0).getPosition());
        Assertions.assertEquals(0, app.players.get(1).getPosition());
    }

    @Test
    public void testBounce(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 62);
            app.players.add(player);
        }
        //rolls 2
        when(drs.roll()).thenReturn(new JSONObject("{\"success\":true,\"dice\":[{\"value\":1,\"type\":\"d6\"},{\"value\":1,\"type\":\"d6\"}]}"));
        when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());

        app.roll(req,res);
        
        Assertions.assertEquals(62, app.players.get(0).getPosition());
    }

    @Test
    public void testJump(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 4);
            app.players.add(player);
        }
        //rolls 2
        when(drs.roll()).thenReturn(new JSONObject("{\"success\":true,\"dice\":[{\"value\":1,\"type\":\"d6\"},{\"value\":1,\"type\":\"d6\"}]}"));
        when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());

        app.roll(req,res);
        
        Assertions.assertEquals(12, app.players.get(0).getPosition());
    }

    @Test
    public void testWin(){
        for(int i = 0; i < 2; i++){
            Player player = new Player("Koen", "kuun" + i, 61);
            app.players.add(player);
        }
        //rolls 2
        when(drs.roll()).thenReturn(new JSONObject("{\"success\":true,\"dice\":[{\"value\":1,\"type\":\"d6\"},{\"value\":1,\"type\":\"d6\"}]}"));
        when(req.params("id")).thenReturn(app.players.get(0).getUuid().toString());

        app.roll(req,res);
        
        Assertions.assertTrue(app.isGameOver());
    }
}
