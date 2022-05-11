package com.example.demo.src.post;

import com.example.demo.src.post.model.GetPostImgRes;
import com.example.demo.src.post.model.GetPostsRes;
import com.example.demo.src.user.model.GetUserPostsRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class PostDao {
    private JdbcTemplate jdbcTemplate;
    private List<GetPostImgRes> getPostImgRes;
    @Autowired
    public void setDataSource(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<GetPostsRes> selectPosts(int userIdx){
        String selectPostsQuery =
                "SELECT p.postIdx, p.userIdx, user.nickName, user.profileImgUrl, p.content,\n" +
                        "       IF(postLikeCount is null, 0, postLikeCount) as postLikeCount,\n" +
                        "       IF(commentCount is null, 0, commentCount) as commentCount,\n" +
                        "       CASE\n" +
                        "        WHEN TIMESTAMPDIFF(SECOND, p.createdAt, current_timestamp) < 60\n" +
                        "            then concat(TIMESTAMPDIFF(SECOND ,p.createdAt, current_timestamp),'초 전')\n" +
                        "        WHEN TIMESTAMPDIFF(MINUTE , p.createdAt, current_timestamp) < 60\n" +
                        "            then concat(TIMESTAMPDIFF(MINUTE , p.createdAt, current_timestamp),'분 전')\n" +
                        "        WHEN TIMESTAMPDIFF(HOUR , p.createdAt, current_timestamp) < 24\n" +
                        "            then concat(TIMESTAMPDIFF(HOUR , p.createdAt, current_timestamp),'시간 전')\n" +
                        "        WHEN TIMESTAMPDIFF(DAY , p.createdAt, current_timestamp) < 365\n" +
                        "            then concat(TIMESTAMPDIFF(DAY , p.createdAt, current_timestamp),'일 전')\n" +
                        "        ELSE concat(TIMESTAMPDIFF(YEAR , p.createdAt, current_timestamp),'년 전')\n" +
                        "        end as updatedAt,\n" +
                        "       IF(pl.stauts = 'ACTIVE', 'Y', 'N') as likedOrNot\n" +
                        "FROM User as u\n" +
                        "    left join (SELECT followerIdx, followeeIdx FROM Follow WHERE status = 'ACTIVE') f on f.followerIdx = u.userIdx\n" +
                        "    left join (SELECT userIdx, postIdx, content, createdAt FROM Post WHERE status = 'ACTIVE') p on p.userIdx = f.followeeIdx\n" +
                        "    join (SELECT userIdx, nickName, profileImgUrl FROM User WHERE status = 'ACTIVE') user on user.userIdx = p.userIdx\n" +
                        "    left join (SELECT postIdx, COUNT(postLikeIdx) as postLikeCount FROM PostLike WHERE stauts = 'ACTIVE' group by postIdx) c on c.postIdx = p.postIdx\n" +
                        "    left join (SELECT postIdx, COUNT(content) as commentCount FROM Comment WHERE status = 'ACTIVE' group by postIdx) comment on comment.postIdx = p.postIdx\n" +
                        "    left join PostLike as pl on pl.userIdx = u.userIdx and pl.postIdx = p.postIdx\n" +
                        "WHERE u.userIdx = ?";
        int selectPostsParam = userIdx;
        return this.jdbcTemplate.query(selectPostsQuery,
                (rs,rowNum) -> new GetPostsRes(
                        rs.getInt("postIdx"),
                        rs.getInt("userIdx"),
                        rs.getString("nickName"),
                        rs.getString("profileImgUrl"),
                        rs.getString("content"),
                        rs.getInt("postLikeCount"),
                        rs.getInt("commentCount"),
                        rs.getString("updatedAt"),
                        rs.getString("likedOrNot"),
                        getPostImgRes = this.jdbcTemplate.query("SELECT pi.postImgUrlIdx, pi.imgUrl\n" +
                                "FROM PostImgUrl as pi\n" +
                                "join Post as p on p.postIdx = pi.postIdx\n" +
                                "WHERE pi.status = 'ACTIVE' and p.postIdx = ?;",
                                (rk, rownum) -> new GetPostImgRes(
                                        rk.getInt("postImgUrlIdx"),
                                        rk.getString("imgUrl")
                                ), rs.getInt("postIdx")
                                )
                ), selectPostsParam);
    }

    public int checkUserExist(int userIdx){
        String checkUserExistQuery = "select exists(select userIdx from User where userIdx = ?)";
        int checkUserExistParams = userIdx;
        return this.jdbcTemplate.queryForObject(checkUserExistQuery,
                int.class,
                checkUserExistParams);

    }

}
