package com.example.demo.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.transport.*;
import org.springframework.http.*;
import org.eclipse.jgit.api.Git;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileReader;
import java.util.*;


@RestController
@RequestMapping(value = "test")
public class apiTest {

    @GetMapping(value = "login")
    public String loginSend(String message){
        //message = "hello";
        String loginUrl = "https://www.weibangong.com/api/profile/login";
        String msgUrl = "https://www.weibangong.com/v2/msg";
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String,Object> info = new HashMap<>();
        info.put("username","18811650018");
        info.put("password","zdd18811650018");
        info.put("runCode",1);
        HttpEntity<Map<String,Object>> requestEntity = new HttpEntity<>(info,headers);
        ResponseEntity<Map> response = client.exchange(loginUrl,method,requestEntity,Map.class);
        String access_token = response.getBody().get("access_token").toString();
        System.out.println(access_token);
        if(response.getStatusCode().equals(HttpStatus.OK)){
            RestTemplate msgClient = new RestTemplate();
            HttpHeaders msgHeaders = new HttpHeaders();
            String auth = "Bearer "+ access_token;
            msgHeaders.add("Authorization",auth);
            JSONObject msgJson = new JSONObject();
            msgJson.put("action","messages/add");
            JSONObject contentJson = new JSONObject();
            JSONObject innerContentJson = new JSONObject();
            innerContentJson.put("type",0);
            innerContentJson.put("text",message);
            String jsonString = innerContentJson.toJSONString();
            contentJson.put("content",jsonString);
            contentJson.put("contentType",0);
            contentJson.put("sourceId",3081054);
            contentJson.put("targetId",3081054);
            contentJson.put("targetType",1);
            contentJson.put("tenantId",1182);
            String uuid = UUID.randomUUID().toString();
            contentJson.put("uuid",uuid);
            msgJson.put("content",contentJson);
            HttpEntity<Map<String,Object>> msgEntity = new HttpEntity<>(msgJson,msgHeaders);
            System.out.println(msgEntity);
           // msgClient.put(msgUrl,msgEntity);

            ResponseEntity<Map> msgResponse = msgClient.exchange(msgUrl, HttpMethod.PUT,msgEntity,Map.class);
            System.out.println(msgResponse.getBody());
            return msgResponse.getStatusCode().toString();
        }
        return "LOGIN ERR";
    }

    @PostMapping(value = "pushApi")
    public String pushApi(@RequestBody JSONObject jsonObject)
    {
//        System.out.println(jsonObject);
        Map<String,Object> pushInfo = new HashMap<>();
        pushInfo.put("提交者",jsonObject.get("user_name"));
        //pushInfo.put("提交者邮箱",jsonObject.get("user_email"));
        pushInfo.put("类型",jsonObject.get("object_kind"));
        pushInfo.put("分支",jsonObject.get("ref"));
        //pushInfo.put("checkout_sha",jsonObject.get("checkout_sha"));
        JSONObject repo_url = jsonObject.getJSONObject("repository");
        pushInfo.put("仓库地址",repo_url.get("url"));
        JSONArray commitsInfo = jsonObject.getJSONArray("commits");
        System.out.println(commitsInfo.size());
        JSONArray commits = new JSONArray();
        ArrayList<String> commits_id = new ArrayList<>();
        for(int i=0;i<commitsInfo.size();i++){
            JSONObject eachCommit = new JSONObject();
            JSONObject each = commitsInfo.getJSONObject(i);
            eachCommit.put("commit_id",each.get("id"));
            eachCommit.put("message",each.get("message"));
            eachCommit.put("url",each.get("url"));
            commits.add(eachCommit);
        }
        for(int i=0;i<commits.size();i++){
            JSONObject each = commits.getJSONObject(i);
            commits_id.add(each.getString("commit_id")); //获取push的commit id
        }
        ArrayList<String> changesInfo = gitOptions(commits_id);
        pushInfo.put("commits",commits);
        String message = "";
        for (String key : pushInfo.keySet()) {
            if(key.equals("commits")){
                message += key +": [{\n";
                for(int i=0;i<commits.size();i++){
                    String info = "";
                    JSONObject each = commits.getJSONObject(i);
                    info += "改动: " + changesInfo.get(i)+"\n";
                    info += "消息: " + each.getString("message");
//                    if(i!=commits.size()-1)
//                        info += "地址: " + each.getString("url")+"\n},\n{\n";
//                    else
                    info += "地址: " + each.getString("url")+"\n}";
                    message += info;
                    break;  //需求只要最近一次的commit信息
                }
                message += "]\n";
            }
            else {
                message += key + ": " + pushInfo.get(key) + "\n";
            }
            //System.out.println(key + ": " + pushInfo.get(key)+"\n");
        }
        return loginSend(message);
    }

    @GetMapping(value = "git")
    public ArrayList<String> gitOptions(ArrayList<String> commits){
        // 本地服务器用这个 具体根据自己配置的环境设置
        commits.add("261b007b9fcddd88e4a20eb4fcf4163309f79c06"); // 测试commit
        commits.add("cbbeda99edd80a8382b4c5a232dca1acd607abd9"); // 测试commit
        String localPath = "/Users/zhangdd/Desktop/Project/repo";
        String sshKeyPath = "/Users/zhangdd/.ssh/id_rsa";
        String knowHostPath = "/Users/zhangdd/.ssh/known_hosts";
        String commitsFilePath = "/Users/zhangdd/Desktop/commits_log";
        String command = "sh /Users/zhangdd/Desktop/test.sh ";

        // 远程服务器用这个
//        String localPath = "/root/download/Project/demo";
//        String sshKeyPath = "/root/.ssh/id_rsa";
//        String knowHostPath = "/root/.ssh/known_hosts";
//        String commitsFilePath = "/root/download/commits_log";
//        String command = "sh /root/download/test.sh ";
        File commitsFile = new File(commitsFilePath);
        if (!commitsFile.exists() && !commitsFile.isDirectory()) {
            System.out.println("未找到commits_log文件夹，正在创建...");
            commitsFile.mkdirs();
        } else {
            System.out.println("文件夹已存在");
        }
        MySShSessionFactory myFactory = new MySShSessionFactory();
        myFactory.setSshKeyFilePath(sshKeyPath);
        myFactory.setKnowHostFilePath(knowHostPath);
        SshSessionFactory.setInstance(myFactory);
        Git git = null;
        try {
            git = Git.open(new File(localPath));
        }catch (Exception e){
            System.out.println("No such repo, need to clone\nwaiting for cloning...");
            try {
                CloneCommand clone = Git.cloneRepository()
                        .setURI("git@git.haizhi.me:zhangdongdong/demo.git")
                        .setDirectory(new File(localPath));
                git = clone.call();
                System.out.println("clone succeed");
            }catch (Exception c){
                System.out.println(c);
                System.out.println("clone failed");
            }
        }
        ArrayList<String> changes = new ArrayList<>();
        if(git != null){
            System.out.println("OK");
            try {
                git.pull().call();
                System.out.println("pull succeed");
            }catch (Exception d){
                d.printStackTrace();
            }
            for(int i=0;i<commits.size();i++){
                String cmd = command +localPath+" "+commits.get(i);
                try {
                    Process ps = Runtime.getRuntime().exec(cmd);
                    ps.waitFor();
                    Scanner sc=new Scanner(new FileReader(commitsFilePath+"/"+commits.get(i)+".txt"));
                    String line;
                    while((sc.hasNextLine()&&(line=sc.nextLine())!=null)) {
                        if (!sc.hasNextLine()) {
                            line = line.replace("files changed","个文件被改动");
                            line = line.replace("file changed","个文件被改动");
                            line = line.replace("insertions","行");
                            line = line.replace("deletions","行");
                            line = line.replace("insertion","行");
                            line = line.replace("deletion","行");
                            changes.add(line);
                            //System.out.println(line);
                        }
                    }
                }catch (Exception e){
                    System.out.println(e);
                }
                break;
            }
            System.out.println("commits log 生成完毕");

        }
        else{
            System.out.println("git error");
        }
        return changes;
    }

}
