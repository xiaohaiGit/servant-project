package org.servantframework.examples.servant;

import org.servantframework.examples.entry.Car;
import org.servantframework.web.annotation.Path;
import org.servantframework.web.annotation.Servant;

@Servant
public class CarServant {

    @Path("/car/get")
    public Car get(int id){
        Car car = new Car();
        car.setId(id);
        car.setSize(2);
        car.setName("audi");
        return car;
    }

}
