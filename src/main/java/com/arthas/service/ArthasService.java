package com.arthas.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ArthasService {

    public void test(int sleep) {
        try {
            TimeUnit.SECONDS.sleep(sleep);
        } catch (Exception e) {

        }
        test1();
        test2();
    }

    public void test2() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (Exception e) {

        }
    }

    private void test1() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (Exception e) {

        }
    }
}
