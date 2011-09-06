package hub;

import static org.easymock.EasyMock.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

public class HubTest extends TestCase {

  HttpServletRequest req;
  HttpServletResponse rsp;
  Hub hub;

  public HubTest() {
  }

  public void setUp() {
    hub = new Hub();
  }

  public void tearDown() {
    hub = null;
  }

  public void testReadFully() throws Exception {
    byte [] buf = "test".getBytes();
    //    assertEquals(new String(buf),
    //                 new String(hub.readFully(new java.io.ByteArrayInputStream(buf))));
  }

  public void testGet() throws Exception {
    req = createMock(HttpServletRequest.class);
    rsp = createMock(HttpServletResponse.class);
    /*
    expect(req.getParameter("key")).andReturn("1");
    replay(req);
    hub.init();
    hub.service(req, rsp);
    hub.doGet(req, rsp);
    verify(req);
    */
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(HubTest.class);
  }
}