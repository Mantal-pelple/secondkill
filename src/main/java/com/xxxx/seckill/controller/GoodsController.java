package com.xxxx.seckill.controller;

import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.vo.DetailVo;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    /**
     *
     * windows优化前QPS：1780.077374029858
     * 缓存页面之后2843.655803901496
     * （秒杀）优化前QPS：1360.7416949398553
     *345.88472123420894
     * 3006.9761847486166
     * @param model
     * @param user
     * @return
     */

    @RequestMapping(value="/toList",produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model, User user,HttpServletRequest request, HttpServletResponse response){
        //从redis获取页面，如果不为空那么返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if(!StringUtils.isEmpty(html)){
            return html;
        }
        model.addAttribute("user",user);
        model.addAttribute("goodsList",goodsService.findGoodsVo());
        //如果为空，手动渲染，放在redis并返回
        WebContext webContext = new WebContext(request,response,request.getServletContext(),request.getLocale(),
                model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);
        if(!StringUtils.isEmpty(html)){
            valueOperations.set("goodsList",html,60, TimeUnit.SECONDS);
        }
//        return "goodsList";


        return html;
    }

    @RequestMapping("/toDetail/{goodsId}")
    @ResponseBody
    public RespBean toDetail(Model model, User user, @PathVariable Long goodsId){
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        int secKillStatus = 0;
        int remainSeconds = 0;
        if(nowDate.before(startDate)){
            remainSeconds = (int)((startDate.getTime()-nowDate.getTime())/1000);
        }else if(nowDate.after(endDate)){
            secKillStatus = 2;
            remainSeconds = -1;
        }else{
            remainSeconds = 0;
            secKillStatus = 1;
        }
        DetailVo detailVo = new DetailVo();
        detailVo.setUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setSecKillStatus(secKillStatus);
        detailVo.setRemainSeconds(remainSeconds);
        return RespBean.success(detailVo);
    }


    @RequestMapping(value = "/toDetail2/{goodsId}",produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toDetail2(Model model, User user, @PathVariable Long goodsId,HttpServletRequest request, HttpServletResponse response){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if(!StringUtils.isEmpty(html)){
            return html;
        }
        model.addAttribute("user",user);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        int secKillStatus = 0;
        int remainSeconds = 0;
        if(nowDate.before(startDate)){
            remainSeconds = (int)((startDate.getTime()-nowDate.getTime())/1000);
        }else if(nowDate.after(endDate)){
            secKillStatus = 2;
            remainSeconds = -1;
        }else{
            remainSeconds = 0;
            secKillStatus = 1;
        }
        model.addAttribute("remainSeconds",remainSeconds);
        model.addAttribute("goods",goodsVo);
        model.addAttribute("secKillStatus",secKillStatus);
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
        if(!StringUtils.isEmpty(html)){
            valueOperations.set("goodsDetail:"+goodsId,html,60, TimeUnit.SECONDS);
        }
        return html;
    }
}
