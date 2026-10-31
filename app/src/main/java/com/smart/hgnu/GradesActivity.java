package com.smart.hgnu;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/** Read-only native grade viewer using the official grade model and field names. */
public final class GradesActivity extends Activity {
    private LinearLayout content;
    private TextView status;
    private Spinner years;
    private CampusController controller;
    private int requestVersion;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        controller = ((CampusState) getApplication()).controller;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 16, 24, 16);
        root.setBackgroundColor(0xfff5f7f2);
        Button back = new Button(this);
        back.setText("‹ 返回服务目录");
        back.setContentDescription("返回服务目录");
        back.setOnClickListener(v -> finish());
        root.addView(back);
        root.addView(label("成绩排名", 23));
        root.addView(label("成绩来自学校教务系统，最终结果以教务处公布为准。", 13));
        years = new Spinner(this);
        years.setContentDescription("选择学年");
        root.addView(years);
        status = label("正在获取学年…", 15);
        root.addView(status);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        controller.loadGrades(
                null,
                new CampusController.GradeCallback() {
                    public void ready(Object data) {
                        if (isFinishing() || isDestroyed()) return;
                        if (!(data instanceof JSONArray)) {
                            failed("学年数据格式异常");
                            return;
                        }
                        JSONArray array = (JSONArray) data;
                        ArrayList<String> values = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            Object value = array.opt(i);
                            if (!(value instanceof String)) {
                                failed("学年数据格式异常");
                                return;
                            }
                            if (!((String) value).trim().isEmpty()) values.add((String) value);
                        }
                        if (values.isEmpty()) {
                            status.setText("学校暂未提供可查询的成绩学年");
                            return;
                        }
                        ArrayAdapter<String> adapter =
                                new ArrayAdapter<>(
                                        GradesActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        values);
                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        years.setAdapter(adapter);
                        years.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    public void onNothingSelected(AdapterView<?> parent) {}

                                    public void onItemSelected(
                                            AdapterView<?> p, View v, int pos, long id) {
                                        loadYear(values.get(pos));
                                    }
                                });
                    }

                    public void failed(String message) {
                        showFailure(message);
                    }
                });
    }

    private void loadYear(String year) {
        int version = ++requestVersion;
        status.setText("正在获取成绩…");
        content.removeAllViews();
        controller.loadGrades(
                year,
                new CampusController.GradeCallback() {
                    public void ready(Object data) {
                        if (isFinishing() || isDestroyed() || version != requestVersion) return;
                        if (!(data instanceof JSONObject)) {
                            failed("成绩数据格式异常");
                            return;
                        }
                        JSONObject grades = (JSONObject) data;
                        JSONArray terms = grades.optJSONArray("XQCJLB");
                        if (terms == null) {
                            failed("学校未返回学期成绩数据");
                            return;
                        }
                        content.addView(
                                label(
                                        "学年绩点："
                                                + value(grades, "XNJD")
                                                + "\n班级排名："
                                                + value(grades, "BJPM")
                                                + "    专业排名："
                                                + value(grades, "ZYPM"),
                                        16));
                        int count = 0;
                        for (int i = 0; i < terms.length(); i++) {
                            JSONObject term = terms.optJSONObject(i);
                            if (term == null) continue;
                            content.addView(
                                    label(
                                            "第 "
                                                    + value(term, "XQ")
                                                    + " 学期 · 绩点 "
                                                    + value(term, "XQJD"),
                                            18));
                            JSONArray courses = term.optJSONArray("CJLB");
                            if (courses == null || courses.length() == 0) {
                                content.addView(label("该学期暂无课程成绩记录", 14));
                                continue;
                            }
                            for (int j = 0; j < courses.length(); j++) {
                                JSONObject course = courses.optJSONObject(j);
                                if (course == null) continue;
                                count++;
                                TextView row =
                                        label(
                                                value(course, "KCMC")
                                                        + "\n综合成绩："
                                                        + ("0"
                                                                        .equals(
                                                                                JsonFields.text(
                                                                                        course,
                                                                                        "SFYD"))
                                                                ? "待查看（请使用学校网页刮卡）"
                                                                : value(course, "XSCJ"))
                                                        + "    学分："
                                                        + value(course, "XF"),
                                                16);
                                row.setPadding(12, 22, 12, 22);
                                row.setOnClickListener(
                                        v ->
                                                new AlertDialog.Builder(GradesActivity.this)
                                                        .setTitle(value(course, "KCMC"))
                                                        .setMessage(
                                                                "课程代码："
                                                                        + value(course, "KCDM")
                                                                        + "\n课程性质："
                                                                        + value(course, "KCXZ")
                                                                        + "\n学分："
                                                                        + value(course, "XF"))
                                                        .setPositiveButton("关闭", null)
                                                        .show());
                                content.addView(row);
                            }
                        }
                        status.setText(count == 0 ? "学校当前未返回该学年的课程成绩记录" : count + " 门课程");
                    }

                    public void failed(String message) {
                        if (version == requestVersion) showFailure(message);
                    }
                });
    }

    private void showFailure(String message) {
        if (!isFinishing() && !isDestroyed()) status.setText(message);
    }

    private String value(JSONObject object, String key) {
        String value = JsonFields.text(object, key);
        return value.isEmpty() ? "暂无" : ("***".equals(value) ? "未公开" : value);
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(0xff173d34);
        view.setPadding(8, 12, 8, 12);
        return view;
    }
}
