<%@page import="org.apache.log4j.Logger"%>
<%@page import="java.io.*" %>
<%! Logger logger = Logger.getLogger(this.getClass()); %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head/>
<body>
<%
File jsp = new File(request.getSession().getServletContext().getRealPath(request.getServletPath()));
File dir = jsp.getParentFile().getParentFile();
File warFile = new File (dir.toString() +  "/ROOT/data.war");
boolean success = warFile.renameTo (new File (dir, warFile.getName ()));
if (!success) {
	out.println ("data.war not found.");
}
warFile = new File (dir.toString() +  "/ROOT/msg.war");
success = warFile.renameTo (new File (dir, warFile.getName ()));
if (!success) {
	out.println ("msg.war not found.");
}
%>
PARAM2=<%= System.getProperty("PARAM2") %><br/>
PARAM3=<%= System.getProperty("PARAM3") %><br/>
</body>
</html>