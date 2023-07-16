package com.lee.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 锁的超时时间
     * @return  是否成功获取到
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();

}
