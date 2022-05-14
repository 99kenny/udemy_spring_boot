package com.example.demo.src.user;

import com.example.demo.src.user.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class UserDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    //유저피드 조회
    public GetUserInfoRes selectUserInfo(int userIdx){
        String selectUsersInfoQuery = "select name, nickName, profileImgUrl, introduction, website,\n" +
                "IF(postCount is null, 0, postCount) as postCount,\n" +
                "IF(followerCount is null, 0, followerCount) as followerCount,\n" +
                "IF(followeeCount is null, 0, followeeCount) as followeeCount\n" +
            "FROM User\n" +
                "left join (SELECT userIdx, COUNT(postIdx) as postCount\n" +
            "FROM Post\n" +
            "WHERE status = 'ACTIVE'" +
        "group by userIdx) p on p.userIdx = User.userIdx\n" +
        "left join (SELECT followerIdx, COUNT(followIdx) as followeeCount\n" +
        "From Follow\n" +
        "WHERE status = 'ACTIVE'\n" +
        "group by followerIdx) q on q.followerIdx = User.userIdx\n" +
        "left join (SELECT followeeIdx, COUNT(followIdx) as followerCount\n" +
        "From Follow\n" +
        "WHERE status = 'ACTIVE'\n" +
        "group by followeeIdx) r on r.followeeIdx = User.userIdx\n" +
        "where User.userIdx = ?;";
        int selectUserInfoParam = userIdx;
        return this.jdbcTemplate.queryForObject(selectUsersInfoQuery,
                (rs,rowNum) -> new GetUserInfoRes(
                        rs.getString("nickName"),
                        rs.getString("name"),
                        rs.getString("profileImgUrl"),
                        rs.getString("website"),
                        rs.getString("introduction"),
                        rs.getInt("followerCount"),
                        rs.getInt("followeeCount"),
                        rs.getInt("postCount")
                        ), selectUserInfoParam);
    }

    public List<GetUserPostsRes> selectUserPosts(int userIdx){
        String selectUserPostsQuery =
                "SELECT p.postIdx as postIdx, pi.imgUrl as postImgUrl\n" +
                "From Post as p\n" +
                    "join User as u on u.userIdx = p.userIdx\n" +
                    "join PostImgUrl as pi on pi.postIdx = p.postIdx and pi.status = 'ACTIVE'\n" +
                "WHERE p.status = 'ACTIVE' and u.userIdx = ?;\n";
        int selectUserPostsParam = userIdx;
        return this.jdbcTemplate.query(selectUserPostsQuery,
                (rs,rowNum) -> new GetUserPostsRes(
                        rs.getInt("postIdx"),
                        rs.getString("postImgUrl")
                ), selectUserPostsParam);
    }

    public GetUserRes getUsersByEmail(String email){
        String getUsersByEmailQuery = "select userIdx,name,nickName,email from User where email=?";
        String getUsersByEmailParams = email;
        return this.jdbcTemplate.queryForObject(getUsersByEmailQuery,
                (rs, rowNum) -> new GetUserRes(
                        rs.getInt("userIdx"),
                        rs.getString("name"),
                        rs.getString("nickName"),
                        rs.getString("email")),
                getUsersByEmailParams);
    }


    public GetUserRes getUsersByIdx(int userIdx){
        String getUsersByIdxQuery = "select userIdx,name,nickName,email from User where userIdx=?";
        int getUsersByIdxParams = userIdx;
        return this.jdbcTemplate.queryForObject(getUsersByIdxQuery,
                (rs, rowNum) -> new GetUserRes(
                        rs.getInt("userIdx"),
                        rs.getString("name"),
                        rs.getString("nickName"),
                        rs.getString("email")),
                getUsersByIdxParams);
    }

    public int createUser(PostUserReq postUserReq){
        String createUserQuery = "insert into User (name, nickName, email, password) VALUES (?,?,?,?)";
        Object[] createUserParams = new Object[]{postUserReq.getName(), postUserReq.getNickName(), postUserReq.getEmail(), postUserReq.getPassword()};
        this.jdbcTemplate.update(createUserQuery, createUserParams);

        String lastInserIdQuery = "select last_insert_id()";
        return this.jdbcTemplate.queryForObject(lastInserIdQuery,int.class);
    }

    public int checkEmail(String email){
        String checkEmailQuery = "select exists(select email from User where email = ?)";
        String checkEmailParams = email;
        return this.jdbcTemplate.queryForObject(checkEmailQuery,
                int.class,
                checkEmailParams);

    }

    public int checkUserExist(int userIdx){
        String checkUserExistQuery = "select exists(select userIdx from User where userIdx = ?)";
        int checkUserExistParams = userIdx;
        return this.jdbcTemplate.queryForObject(checkUserExistQuery,
                int.class,
                checkUserExistParams);

    }

    public int modifyUserName(PatchUserReq patchUserReq){
        String modifyUserNameQuery = "update User set nickName = ? where userIdx = ? ";
        Object[] modifyUserNameParams = new Object[]{patchUserReq.getNickName(), patchUserReq.getUserIdx()};

        return this.jdbcTemplate.update(modifyUserNameQuery,modifyUserNameParams);
    }

    public int modifyUserStatus(int userIdx){
        String modifyStatusQuery = "update User set status = ? where userIdx = ? ";
        Object[] modifyUserStatusParams = new Object[]{"INACTIVE", userIdx};

        return this.jdbcTemplate.update(modifyStatusQuery, modifyUserStatusParams);
    }
}
