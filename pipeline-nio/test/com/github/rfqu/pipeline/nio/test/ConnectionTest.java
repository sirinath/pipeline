package com.github.rfqu.pipeline.nio.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.pipeline.core.Pipeline;
import com.github.rfqu.pipeline.core.SinkNode;
import com.github.rfqu.pipeline.nio.AsyncServerSocketChannel;
import com.github.rfqu.pipeline.nio.AsyncSocketChannel;
import com.github.rfqu.pipeline.util.Connection;

public  class ConnectionTest {
    static final int BUF_SIZE = 128;
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);

    @BeforeClass
    public static void initClass() {
        DFContext.setSingleThreadExecutor();
    }

    AsyncServerSocketChannel assc; 
    Connection serverConn;
    Connection clientConn;
        
    @Before
    public void init() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        assc = new AsyncServerSocketChannel(local9990, 20);
        Pipeline acceptor=new Pipeline();
        acceptor.setSource(assc).setSink(new Reactor()).start();
        
    	AsyncSocketChannel client=new AsyncSocketChannel(local9990);
    	clientConn=new Connection(client);
    	clientConn.start();
    	
    	serverConn=(Connection) acceptor.get();//(1000);
    }

    @After
    public void close() {
    	clientConn.close();
    	serverConn.close();
    	assc.close();
    }
    
    /**
     * send a message from client to server 
     */
    @Test
    public void smokeIOTest() throws Exception {
    	try {
            String message="hi there";
            clientConn.write(message);
            clientConn.write("\n");
            String reply=serverConn.read();
            assertEquals(message, reply);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * send a message from server to client, then close connection
     */
//    @Test
    public void smokeIOTest1() throws Exception {
    	String message="hi there";
    	serverConn.write(message);
    	serverConn.close();
		String reply=clientConn.read();
    	assertEquals(message, reply);
    }
    
    /**
     * send 2 messages in both directions simultaneousely 
     */
//    @Test
    public void smokeIOTest2() throws Exception {
    	String message="hi there";
    	clientConn.write(message);
    	serverConn.write(message);
    	clientConn.write("\n");
    	serverConn.write("\n");
		String reply1=serverConn.read();
		String reply2=clientConn.read();
    	clientConn.close();
    	serverConn.close();
    	assertEquals(message, reply1);
    	assertEquals(message, reply2);
    }
    
    /**
     * accepted connections, formatted as {@link AsyncSocketChannel}, arrive to {@link myInput}.
     * They should be returned to the peer {@link AsyncServerSocketChannel}.
     * For each connection, an instance of {$link Connection} is created.
     */
    class Reactor extends SinkNode<AsyncSocketChannel> {
        
        @Override
        protected void act(AsyncSocketChannel channel) {
            Connection connection;
            try {
                connection = new Connection(channel);
            } catch (IOException e) {
                throw new RuntimeException();
            }
            connection.start();
            context.post(connection);
        }
        
    }
    
    public static void main(String[] args) {
        ConnectionTest ct = new ConnectionTest();
        ConnectionTest.initClass();
        try {
            ct.init();
            ct.smokeIOTest();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}