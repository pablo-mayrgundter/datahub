package hub;

import javax.servlet.http.HttpServletRequest;

class Resource {

  final String dir;
  Long id;

  Resource(String kind) {
    dir = kind;
  }

  Resource(HttpServletRequest req) {
    // The servlet's prefix
    String srvPath = req.getServletPath();

    // The entire URI path.
    String uriPath = req.getRequestURI();
    // Chop out the suffix for this request.
    String idPath = uriPath.substring(srvPath.length());
    // This should start with a slash and have no other slashes.
    String [] parts = idPath.split("/");
    // So the rest of idPath starts with a dir.
    dir = parts[1];
    // And may contain an ID if referencing an existing resource.
    if (parts.length == 3) {
      id = new Long(parts[2]);
    }
  }

  String getDir() {
    return dir;
  }

  boolean hasId() {
    return id != null;
  }

  long getId() {
    if (!hasId()) {
      throw new IllegalStateException("Resource has no ID");
    }
    return id;
  }
}
