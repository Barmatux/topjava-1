package ru.javawebinar.topjava.repository.jdbc;


import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.javawebinar.topjava.model.Meal;
import ru.javawebinar.topjava.repository.MealRepository;
import ru.javawebinar.topjava.web.SecurityUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
public class JdbcMealRepository implements MealRepository {

    private static final BeanPropertyRowMapper<Meal> ROW_MAPPER = BeanPropertyRowMapper.newInstance(Meal.class);

    private final JdbcTemplate jdbcTemplate;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final SimpleJdbcInsert insertMeal;
    private static final Logger log = getLogger(JdbcMealRepository.class);

    @Autowired
    public JdbcMealRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.insertMeal = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("meals")
                .usingGeneratedKeyColumns("id");

        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Meal save(Meal meal, int userId) {
        MapSqlParameterSource map = new MapSqlParameterSource()
                .addValue("userId", SecurityUtil.authUserId())
                .addValue("id", meal.getId())
                .addValue("datetime", meal.getDate())
                .addValue("description", meal.getDescription())
                .addValue("calories", meal.getCalories());
        if (meal.isNew()) {
            Number newKey = insertMeal.executeAndReturnKey(map);
            meal.setId(newKey.intValue());
        } else if (namedParameterJdbcTemplate.update(
                "UPDATE meals SET datetime=:datetime, description=:description, calories=:calories, id=:id WHERE userId=:userId and id=:id"
                , map) == 0) {
            return null;
        }
        return meal;
    }

    @Override
    public boolean delete(int id, int userId) {
        return jdbcTemplate.update("DELETE FROM meals" +
                " WHERE id=?", id) != 0;
    }

    @Override
    public Meal get(int id, int userId) {
        return jdbcTemplate.queryForObject("SELECT * FROM meals WHERE id=?", ROW_MAPPER, id);
    }

    @Override
    public List<Meal> getAll(int userId) {

        return jdbcTemplate.query("SELECT * FROM meals WHERE userId=? ORDER BY datetime", ROW_MAPPER, userId);
    }

    @Override
    public List<Meal> getBetween(LocalDateTime startDate, LocalDateTime endDate, int userId) {
        try {
            return jdbcTemplate.query("SELECT * FROM meals WHERE userId=? AND  (datetime >= ?" +
                    "  AND datetime < ?) ORDER BY datetime", ROW_MAPPER, userId, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
        } catch (Exception e) {
            log.error("Something bad happened with date", e);
            return Collections.emptyList();
        }

    }
}
