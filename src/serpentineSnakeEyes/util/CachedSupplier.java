package serpentineSnakeEyes.util;

import serpentineSnakeEyes.robot.Robot;

import java.util.function.Supplier;

public class CachedSupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;

    private T cache;
    private int cachedRound = -1;

    public CachedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }


    @Override
    public T get() {
        if (cachedRound != Robot.rc.getRoundNum()) {
            cache = supplier.get();
            cachedRound = Robot.rc.getRoundNum();
        }
        return cache;
    }
}
