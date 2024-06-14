package org.servantframework.examples.servant;

import org.servantframework.examples.entry.User;
import org.servantframework.web.annotation.Path;
import org.servantframework.web.annotation.Servant;

@Servant
public class UserServant {

    @Path("/user/get")
    public User get(int id) {
        User user = new User();
        user.setId(id);
        user.setName("shawn");
        user.setAge(100);
        return user;
    }

}
