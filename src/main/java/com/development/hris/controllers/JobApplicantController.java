package com.development.hris.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.development.hris.entities.OpenJob;
import com.development.hris.entities.OpenJobComparator;
import com.development.hris.service.ControllerUtilities;
import com.development.hris.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class JobApplicantController {

    private final UserService userService;
    private final ControllerUtilities controllerUtilities;

    @GetMapping("/candidateHome")
    public String getJobsAsApplicant(Model model, @RequestParam Map<String, String> params){

        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
        
        List<OpenJob> allJobs = userService.getAllJobs();
        allJobs = allJobs.stream().filter(p -> p.isActive() && p.getDeadline().after(new Date())).collect(Collectors.toList());
        Collections.sort(allJobs, new OpenJobComparator());
        Collections.reverse(allJobs);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<OpenJob> temp = new ArrayList<OpenJob>(allJobs);
            for (OpenJob submission : temp) {
                if(!submission.getPosition().contains(searchTerm) && !submission.getDeadline().toString().contains(searchTerm) && !submission.getPostDate().toString().contains(searchTerm)){
                    allJobs.remove(submission);
                }
            }
        }
        int totalPages = (int)Math.ceil(allJobs.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<OpenJob> toDisplay = new ArrayList<OpenJob>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allJobs.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        controllerUtilities.prepareBaseModel(model, "NOT_APPLICABLE", null);
        controllerUtilities.preparePagingModel(model, null, null, nextPage, prevPage, searchTerm, userService.getAllJobs().size(), allJobs.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "postings", toDisplay, false, null, null);
        model.addAttribute("hrEmail", controllerUtilities.getHrEmail());
        return "viewTemplate";
    }
}
