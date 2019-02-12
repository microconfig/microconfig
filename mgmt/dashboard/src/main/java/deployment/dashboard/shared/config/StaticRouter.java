package deployment.dashboard.shared.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class StaticRouter {
    @RequestMapping({"/", "/dashboard", "/group", "logs"})
    public String route(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/") ? "/dashboard" : path;
    }
}