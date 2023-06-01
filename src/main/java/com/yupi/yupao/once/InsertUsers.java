package com.yupi.yupao.once;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import com.yupi.yupao.mapper.UserMapper;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
public class InsertUsers {
    @Resource
    private UserService userService;

    /**
     * 批量插入数据
     */
//    @Scheduled(initialDelay = 5000,fixedRate = Long.MAX_VALUE)
    private void doInsertUsers(){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int batchSize = 5000;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            List<User> userList = new ArrayList<>();
            do {
                j++;
                User user = new User();
                user.setUsername("甲用户");
                user.setUserAccount("fakeUser");
                user.setAvatarUrl("https://img.ddtouxiang.com/upload/touxiang/20230306/0306072840247.jpeg");
                user.setGender(0);
                user.setUserPassword("cb6b340fed7312769fd3555b21618d4c");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("11111");
                user.setTags("[]");
                user.setProfile("hh");
                userList.add(user);
            } while (j % batchSize != 0);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        System.out.println("threadName: " + Thread.currentThread().getName());
                        userService.saveBatch(userList, batchSize);
                    }
                    , threadPoolExecutor);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }



}
