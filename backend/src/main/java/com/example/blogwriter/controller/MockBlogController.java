package com.example.blogwriter.controller;

import com.example.blogwriter.model.BlogPost;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/mock-blog")
public class MockBlogController {

    // Simple in-memory storage for mock blog posts
    public static final List<BlogPost> posts = new ArrayList<>();

    static {
        posts.add(new BlogPost("반갑습니다! 첫 번째 테스트 포스트입니다.", "여기는 모의 블로그 공간입니다. Playwright가 자동으로 로그인하고 이곳에 글을 쓸 것입니다.", "테스트, 첫글"));
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("loggedIn") != null && (boolean) session.getAttribute("loggedIn")) {
            return "redirect:/mock-blog/write";
        }
        return "mock-blog/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        if ("admin".equals(username) && "admin".equals(password)) {
            session.setAttribute("loggedIn", true);
            return "redirect:/mock-blog/write";
        }
        model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다. (admin / admin)");
        return "mock-blog/login";
    }

    @GetMapping("/write")
    public String writePage(HttpSession session) {
        if (session.getAttribute("loggedIn") == null || !(boolean) session.getAttribute("loggedIn")) {
            return "redirect:/mock-blog/login";
        }
        return "mock-blog/write";
    }

    @PostMapping("/write")
    public String doWrite(@RequestParam String title,
                          @RequestParam String content,
                          @RequestParam String tags,
                          HttpSession session) {
        if (session.getAttribute("loggedIn") == null || !(boolean) session.getAttribute("loggedIn")) {
            return "redirect:/mock-blog/login";
        }
        posts.add(new BlogPost(title, content, tags));
        return "redirect:/mock-blog/posts";
    }

    @GetMapping("/posts")
    public String postsPage(Model model) {
        model.addAttribute("posts", posts);
        return "mock-blog/posts";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/mock-blog/login";
    }

    @ResponseBody
    @GetMapping("/api/posts")
    public List<BlogPost> getPostsApi() {
        return posts;
    }
}
