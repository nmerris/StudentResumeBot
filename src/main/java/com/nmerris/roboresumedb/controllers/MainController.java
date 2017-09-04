package com.nmerris.roboresumedb.controllers;

import com.nmerris.roboresumedb.CurrPerson;
import com.nmerris.roboresumedb.Utilities;
import com.nmerris.roboresumedb.models.*;
import com.nmerris.roboresumedb.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

import org.joda.time.DateTime;

@Controller
public class MainController {

    @Autowired
    PersonRepo personRepo;
    @Autowired
    EducationRepo educationRepo;
    @Autowired
    SkillRepo skillRepo;
    @Autowired
    WorkExperienceRepo workExperienceRepo;
    @Autowired
    CourseRepo courseRepo;

    @Autowired
    CurrPerson currPerson;


    @GetMapping("/login")
    public String login() {
        return "login";
    }


    @GetMapping("/logout")
    public String logout() {
        return "login";
    }

    // default route takes user to addperson, but since basic authentication security is enabled, they will have to
    // go through the login route first, then Spring will automatically take them to addperson
    @GetMapping("/")
    public String indexPageGet() {
        // redirect is like clicking a link on a web page, this route will not even show a view, it just redirects
        // the user to the addperson route
        return "redirect:/studentdirectory";
    }


    // wipes all the skills, work experiences, and eds from current Person
    @GetMapping("/startover")
    // Transactional is necessary to call removeAllBy.. on the repos
    // PersistenceContext defaults to PersistenceContextType.TRANSACTION, thank you Stack Overflow!
    @Transactional
    public String startOver() {
        // remove all items from Person
        Person p = personRepo.findOne(currPerson.getPersonId());
        p.removeAllEdAchievements();
        p.removeAllWorkExperiences();
        p.removeAllSkills();

        // remove all items from repos
        educationRepo.removeAllByMyPersonIs(p);
        skillRepo.removeAllByMyPersonIs(p);
        workExperienceRepo.removeAllByMyPersonIs(p);

        return "redirect:/editdetails";
    }


    // to get here, a user must have clicked on an existing students summary --> edit resume link, so the student must already exist
    @GetMapping("/addperson")
    public String addPersonGet(Model model) {
        System.out.println("=============================================================== just entered /addperson GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // send the existing person to the form
        model.addAttribute("newPerson", personRepo.findOne(currPerson.getPersonId()));

        NavBarState pageState = getPageLinkState();
        // set the navbar to highlight the appropriate link
        pageState.setHighlightPersonNav(true);
        model.addAttribute("pageState", pageState);

        return "addperson";
    }


    @PostMapping("/addperson")
    public String addPersonPost(@Valid @ModelAttribute("newPerson") Person personFromForm,
                                BindingResult bindingResult, Model model) {
        System.out.println("=============================================================== just entered /addperson POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // return the same view (now with validation error messages) if there were any validation problems
        if(bindingResult.hasErrors()) {
            // always need to set up the navbar, every time a view is returned
            NavBarState pageState = getPageLinkState();
            pageState.setHighlightPersonNav(true);
            model.addAttribute("pageState", pageState);
            return "addperson";
        }

        // as far as I understand it, the Person coming in to this method from the form is NOT the same as the
        // Person we sent to the model in /update/id... so even though personFromForm has the updated first and
        // last names and email, it looses ALL of it's courses.  I could not figure out how to pass the set of courses through the form.
        // So if you just save personFromForm here, you loose all
        // courses.  I also tried: storing the Persons courses in currPerson session variable, then adding it back to
        // personFromForm here.... the problem is that then I get a merge conflict error... the same darn error I was
        // getting on Friday in class, because it thinks they are two different Persons.  So my solution is to get
        // Person p back out from the repo, then update it's fields, and save it.  No need to add the courses back, because
        // they never went anywhere.  Hmmmmmmmmmmmmmmm...................
        Person p = personRepo.findOne(currPerson.getPersonId());
        p.setNameFirst(personFromForm.getNameFirst());
        p.setNameLast(personFromForm.getNameLast());
        p.setEmail(personFromForm.getEmail());
        personRepo.save(p);

        // go to education section automatically, it's the most logical
        // since there is no confirmation page for addperson, we want to redirect here
        // redirect means that if this route gets to this point, it's not even going to return a view at all, which
        // is why no model stuff is needed here, redirect is basically like clicking on a link on a web page
        // you can redirect to any internal route, or any external URL
        return "redirect:/addeducation";
    }


    @GetMapping("/addeducation")
    public String addEdGet(Model model) {
        System.out.println("=============================================================== just entered /addeducation GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        // disable the submit button if >= 10 records in db, it would never be possible for the user to click to get
        // here from the navi page if there were already >= 10 records, however they could manually type in the URL
        // so I want to disable the submit button if they do that and there are already 10 records
        model.addAttribute("disableSubmit", educationRepo.countAllByMyPersonIs(p) >= 10);
//        model.addAttribute("disableSubmit", educationRepo.count() >= 10);

        // each resume section (except personal) shows a running count of the number of records currently in the db
        model.addAttribute("currentNumRecords", educationRepo.countAllByMyPersonIs(p)); // where is my cute little 'o:'?

        NavBarState pageState = getPageLinkState();
        pageState.setHighlightEdNav(true);
        model.addAttribute("pageState", pageState);

        // the users name is displayed at the top of each resume section (except personal details), we need to check
        // every time a view is returned, because the user can change their personal details at any time, and we want
        // to make sure the displayed name is always up to date
        addPersonNameToModel(model);

        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% created new ea, attached currPerson to it, about to add it to model");
        // create a new ea, attach the curr person to it, and add it to model
        EducationAchievement ea = new EducationAchievement();
        ea.setMyPerson(p);
        model.addAttribute("newEdAchievement", ea);

        return "addeducation";
    }

    
    @PostMapping("/addeducation")
    public String addEdPost(@Valid @ModelAttribute("newEdAchievement") EducationAchievement educationAchievement,
                            BindingResult bindingResult, Model model) {
        System.out.println("=============================================================== just entered /addeducation POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        // get the current count from educationRepo for the current Person
        long count = educationRepo.countAllByMyPersonIs(p);
        System.out.println("=========================================== repo count for currPerson is: " + count);

        // the persons name is show at the top of each 'add' section AND each confirmation page, so we want to add
        // it to the model no matter which view is returned
        addPersonNameToModel(model);

        // return the same view (now with validation error messages) if there were any validation problems
        if(bindingResult.hasErrors()) {
            // update the navbar state and add it to our model
            NavBarState pageState = getPageLinkState();
            pageState.setHighlightEdNav(true);
            model.addAttribute("pageState", pageState);

            // disable the form submit button if there are 10 or more records in the education repo
            model.addAttribute("disableSubmit", count >= 10);
            model.addAttribute("currentNumRecords", count);

            return "addeducation";
        }

        // I'm being picky here, but it is possible for the user to refresh the page, which bypasses the form submit
        // button, and so they would be able to add more than 10 items, to avoid this, just condition the db save on count
        if(count < 10) {
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% about to save ea to educationRepo");
            educationRepo.save(educationAchievement);

            // need to get an updated edsCount after saving to repo
            count = educationRepo.countAllByMyPersonIs(p);
            System.out.println("=========================================== repo count for currPerson is: " + count);
        }

        // need to get the count AFTER successfully adding to db, so it is up to date
        model.addAttribute("currentNumRecords", count);

        // add the EducationAchievement just entered to the model, so we can show a confirmation page
        model.addAttribute("edAchievementJustAdded", educationAchievement);
        
        // also need to set disableSubmit flag AFTER adding to db, or user will think they can add more than 10
        // because the 'add another' button will work, but then the entry form button will be disabled, this
        // way the user will not be confused... I am repurposing 'disableSubmit' here, it's actually being used to
        // disable the 'Add Another' button in the confirmation page
        model.addAttribute("disableSubmit", count >= 10);

        // the navbar state depends on the db table counts in various ways, so update after db changes
        NavBarState pageState = getPageLinkState();
        pageState.setHighlightEdNav(true);
        model.addAttribute("pageState", pageState);

        return "addeducationconfirmation";
    }


    // logic in this route is identical to /addeducation, see /addeducation GetMapping for explanatory comments
    @GetMapping("/addworkexperience")
    public String addWorkGet(Model model) {
        System.out.println("=============================================================== just entered /addworkexperience GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        model.addAttribute("disableSubmit", workExperienceRepo.countAllByMyPersonIs(p) >= 10);
        model.addAttribute("currentNumRecords", workExperienceRepo.countAllByMyPersonIs(p));

        NavBarState pageState = getPageLinkState();
        pageState.setHighlightWorkNav(true);
        model.addAttribute("pageState", pageState);

        addPersonNameToModel(model);

        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% created new workExp, attached currPerson to it, about to add it to model");
        WorkExperience workExp = new WorkExperience();
        workExp.setMyPerson(p);
        model.addAttribute("newWorkExperience", workExp);

        return "addworkexperience";
    }
    
    
    // logic in this route is identical to /addeducation, see /addeducation PostMapping for explanatory comments
    @PostMapping("/addworkexperience")
    public String addWorkPost(@Valid @ModelAttribute("newWorkExperience") WorkExperience workExperience,
                            BindingResult bindingResult, Model model) {
        System.out.println("=============================================================== just entered /addworkexperience POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        // get the current count from work repo for the current Person
        long count = workExperienceRepo.countAllByMyPersonIs(p);
        System.out.println("=========================================== repo count for currPerson is: " + count);

        addPersonNameToModel(model);

        if(bindingResult.hasErrors()) {
            NavBarState pageState = getPageLinkState();
            pageState.setHighlightWorkNav(true);
            model.addAttribute("pageState", pageState);
            model.addAttribute("currentNumRecords", count);
            model.addAttribute("disableSubmit", count >= 10);

            return "addworkexperience";
        }

        if(count < 10) {
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% about to save workExp to workExpRepo");
            workExperienceRepo.save(workExperience);

            count = workExperienceRepo.countAllByMyPersonIs(p);
            System.out.println("=========================================== repo count for currPerson is: " + count);
        }

        model.addAttribute("currentNumRecords", count);

        // work experience end date can be left null by user, in which case we want to show 'Present' in the
        // confirmation page
        model.addAttribute("dateEndString", Utilities.getMonthDayYearFromDate(workExperience.getDateEnd()));
        model.addAttribute("workExperienceJustAdded", workExperience);
        model.addAttribute("disableSubmit", count >= 10);

        NavBarState pageState = getPageLinkState();
        pageState.setHighlightWorkNav(true);
        model.addAttribute("pageState", pageState);

        return "addworkexperienceconfirmation";
    }

    
    // logic in this route is identical to /addeducation, see /addeducation GetMapping for explanatory comments
    @GetMapping("/addskill")
    public String addSkillGet(Model model) {
        System.out.println("=============================================================== just entered /addskill GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        model.addAttribute("disableSubmit", skillRepo.countAllByMyPersonIs(p) >= 20);
        model.addAttribute("currentNumRecords", skillRepo.countAllByMyPersonIs(p));

        NavBarState pageState = getPageLinkState();
        pageState.setHighlightSkillNav(true);
        model.addAttribute("pageState", pageState);

        addPersonNameToModel(model);

        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% created new skill, attached currPerson to it, about to add it to model");
        Skill skill = new Skill();
        skill.setMyPerson(p);
        model.addAttribute("newSkill", skill);

        return "addskill";
    }


    // logic in this route is identical to /addeducation, see /addeducation PostMapping for explanatory comments
    @PostMapping("/addskill")
    public String addSkillPost(@Valid @ModelAttribute("newSkill") Skill skill,
                              BindingResult bindingResult, Model model) {
        System.out.println("=============================================================== just entered /addskill POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        // get the current count from work repo for the current Person
        long count = skillRepo.countAllByMyPersonIs(p);
        System.out.println("=========================================== repo count for currPerson is: " + count);

        addPersonNameToModel(model);

        if(bindingResult.hasErrors()) {
            NavBarState pageState = getPageLinkState();
            pageState.setHighlightSkillNav(true);
            model.addAttribute("pageState", pageState);
            model.addAttribute("currentNumRecords", count);
            model.addAttribute("disableSubmit", count >= 20);

            return "addskill";
        }

        if(skillRepo.count() < 20) {
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% about to save skill to Repo");
            skillRepo.save(skill);

            count = skillRepo.countAllByMyPersonIs(p);
            System.out.println("=========================================== repo count for currPerson is: " + count);
        }

        NavBarState pageState = getPageLinkState();
        pageState.setHighlightSkillNav(true);
        model.addAttribute("pageState", pageState);

        model.addAttribute("currentNumRecords", count);
        model.addAttribute("skillJustAdded", skill);
        model.addAttribute("disableSubmit", count >= 20);

        return "addskillconfirmation";
    }


    // this route returns a view that shows ALL the records from every repo
    // every record can be edited by clicking a link next to it
    // every record (except the single personal details record) can also be deleted by clicking a link next to it
    @GetMapping("/editdetails")
    public String editDetails(Model model) {
        System.out.println("=============================================================== just entered /editdetails GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());
        model.addAttribute("person", p);
        model.addAttribute("edAchievements", educationRepo.findAllByMyPersonIs(p));
        model.addAttribute("workExperiences", workExperienceRepo.findAllByMyPersonIs(p));
        model.addAttribute("skills", skillRepo.findAllByMyPersonIs(p));

        NavBarState pageState = getPageLinkState();
        pageState.setHighlightEditNav(true);
        model.addAttribute("pageState", pageState);

        return "editdetails";
    }


    // id is the id to delete
    // type is what table to delete from
    // this route is triggered when the user clicks on the 'delete' link next to a row in editdetails.html
    // no model is needed here because all the returned views are redirects
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") long id, @RequestParam("type") String type)
    {
        System.out.println("=============================================================== just entered /delete/{id} GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        Person p = personRepo.findOne(currPerson.getPersonId());

        try {
            switch (type) {
                case "ed" :
                    // remove the ed from person, then delete it from it's repo
                    p.removeEdAchievement(educationRepo.findOne(id));
                    educationRepo.delete(id);
                    // return with an anchor tag so that the user is still at the same section after deleting
                    // this is not perfect, but it's better than jumping to the top of the page each time
                    return "redirect:/editdetails#education";
                case "person" :
                    personRepo.delete(id); // is this all?
                    return "redirect:/";// TODO make the 'admin' page the default route
                case "workexp" :
                    p.removeWorkExperience(workExperienceRepo.findOne(id));
                    workExperienceRepo.delete(id);
                    return "redirect:/editdetails#workexperiences";
                case "skill" :
                    p.removeSkill(skillRepo.findOne(id));
                    skillRepo.delete(id);
                    return "redirect:/editdetails#skills";
            }
        } catch (Exception e) {
            // need to catch an exception that may be thrown if user refreshes the page after deleting an item.
            // refreshing the page will attempt to delete the same ID from the db, which will not exist anymore if
            // they just deleted it.  catching the exception will prevent the app from crashing, and the same page
            // will simply be redisplayed
        }

        // should never happen, but need it to compile, better to redirect, just in case something does go wrong, at
        // least this way the app will not crash
        return "redirect:/editdetails";
    }


    // id is the id to update
    // type is what table to update
    // this route is triggered when the user clicks on the 'update' link next to a row in editdetails.html
    @GetMapping("/update/{id}")
    public String update(@PathVariable("id") long id, @RequestParam("type") String type, Model model)
    {
        System.out.println("=============================================================== just entered /update/{id} GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // no matter what view is returned, we ALWAYS will allow the submit button to work, since the form that is
        // displays can only contain a record that already exists in a repo
        model.addAttribute("disableSubmit", false);
        addPersonNameToModel(model);

        NavBarState pageState = getPageLinkState();

        // get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        switch (type) {
            case "person" :
                // get the appropriate record from the repo
                model.addAttribute("newPerson", p);
                // set the appropriate nav bar highlight
                pageState.setHighlightPersonNav(true);
                // add the navbar state object to the model
                model.addAttribute("pageState", pageState);
                // return the appropriate view
                return "addperson";
            case "ed" :
                model.addAttribute("newEdAchievement", educationRepo.findOne(id));
                model.addAttribute("currentNumRecords", educationRepo.countAllByMyPersonIs(p));
                pageState.setHighlightEdNav(true);
                model.addAttribute("pageState", pageState);
                return "addeducation";
            case "workexp" :
                model.addAttribute("newWorkExperience", workExperienceRepo.findOne(id));
                model.addAttribute("currentNumRecords", workExperienceRepo.countAllByMyPersonIs(p));
                pageState.setHighlightWorkNav(true);
                model.addAttribute("pageState", pageState);
                return "addworkexperience";
            case "skill" :
                model.addAttribute("newSkill", skillRepo.findOne(id));
                model.addAttribute("currentNumRecords", skillRepo.countAllByMyPersonIs(p));
                pageState.setHighlightSkillNav(true);
                model.addAttribute("pageState", pageState);
                return "addskill";
            case "course" :
                model.addAttribute("newCourse", courseRepo.findOne(id));
                // see boolean flag to highlight the appropriate navbar link
                model.addAttribute("highlightDirectory", false);
                model.addAttribute("highlightCourses", false);
                model.addAttribute("highlightAddCourse", true);
                model.addAttribute("highlightAddStudent", false);
                return "addcourse";
            case "student" :
                model.addAttribute("newPerson", p);
                model.addAttribute("highlightDirectory", false);
                model.addAttribute("highlightCourses", false);
                model.addAttribute("highlightAddCourse", false);
                model.addAttribute("highlightAddStudent", true);
                return "addstudent";
        }

        // should never happen, but need it to compile, better to redirect, just in case something does go wrong, at
        // least this way the app will not crash
        return"redirect:/editdetails";
    }


    @GetMapping("/finalresume")
    public String finalResumeGet(Model model) {
        NavBarState pageState = getPageLinkState();
        pageState.setHighlightFinalNav(true);
        model.addAttribute("pageState", pageState);

        Person p = personRepo.findOne(currPerson.getPersonId());

        // populate the empty ArrayLists in our single Person from data in other tables
//        composePerson(p);

        model.addAttribute("person", p);

        return "finalresume";
    }


    @GetMapping("/studentdirectory")
    public String studentDirectory(Model model) {
        System.out.println("=============================================================== just entered /studentdirectory GET");

        // add all the persons to the model
        model.addAttribute("students", personRepo.findAll());

        // see boolean flag to highlight the appropriate navbar link
        model.addAttribute("highlightDirectory", true);
        model.addAttribute("highlightCourses", false);
        model.addAttribute("highlightAddCourse", false);
        model.addAttribute("highlightAddStudent", false);

        return "studentdirectory";
    }


    @GetMapping("/courselist")
    public String courseListGet(Model model) {
        System.out.println("=============================================================== just entered /courselist GET");

        // add all the persons to the model
        model.addAttribute("courses", courseRepo.findAll());

        // see boolean flag to highlight the appropriate navbar link
        model.addAttribute("highlightDirectory", false);
        model.addAttribute("highlightCourses", true);
        model.addAttribute("highlightAddCourse", false);
        model.addAttribute("highlightAddStudent", false);

        return "courselist";
    }


    // this route only fires if a brand new student is being entered, if an existing student is being entered, it will
    // always come through the /update/id GET route, so always set the currPerson id to zero here
    @GetMapping("/addstudent")
    public String addStudentGet(Model model) {
        System.out.println("=============================================================== just entered /addstudent GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());
        System.out.println("================================== about to create new Person and send it to form");



        Person person = new Person();
        model.addAttribute("newPerson", person);

        // see boolean flag to highlight the appropriate navbar link
        model.addAttribute("highlightDirectory", false);
        model.addAttribute("highlightCourses", false);
        model.addAttribute("highlightAddStudent", true);
        model.addAttribute("highlightAddCourse", false);

        return "addstudent";
    }


    // very similar to /addperson, except this happens in the admin section of the app, not in the resume section
    // the same Person entity is used in both cases, so updating in either place will have the same database results
    @PostMapping("/addstudent")
    public String addStudentPost(@Valid @ModelAttribute("newPerson") Person personFromForm,
                                BindingResult bindingResult, Model model) {
        System.out.println("=============================================================== just entered /addstudent POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        if(bindingResult.hasErrors()) {
            model.addAttribute("highlightDirectory", false);
            model.addAttribute("highlightCourses", false);
            model.addAttribute("highlightAddCourse", false);
            model.addAttribute("highlightAddStudent", true);
            return "addstudent";
        }

        // as far as I understand it, the Person coming in to this method from the form is NOT the same as the
        // Person we sent to the model in /update/id... so even though personFromForm has the updated first and
        // last names and email, it looses ALL of it's courses.  I could not figure out how to pass the set of courses through the form.
        // So if you just save personFromForm here, you loose all
        // courses.  I also tried: storing the Persons courses in currPerson session variable, then adding it back to
        // personFromForm here.... the problem is that then I get a merge conflict error... the same darn error I was
        // getting on Friday in class, because it thinks they are two different Persons.  So my solution is to get
        // Person p back out from the repo, then update it's fields, and save it.  No need to add the courses back, because
        // they never went anywhere.  Hmmmmmmmmmmmmmmm...................
        if(personFromForm.getId() == 0) {
            // a brand new person is being entered, so just save it.. it can't have anything attached to it at this point
            // and so there are no courses to keep track of, just use whatever came in from the form,
            // and update currPerson ID
            currPerson.setPersonId(personRepo.save(personFromForm).getId());
            System.out.println("======================== JUST CREATED NEW PERSON, RESET currPerson id to: " + currPerson.getPersonId());
        }
        else {
            Person p = personRepo.findOne(currPerson.getPersonId());
            p.setNameFirst(personFromForm.getNameFirst());
            p.setNameLast(personFromForm.getNameLast());
            p.setEmail(personFromForm.getEmail());
            personRepo.save(p);
        }

        // go to student directory page if successfully added a student, no need for confirmation page here
        return "redirect:/studentdirectory";
    }


    @GetMapping("/addcourse")
    public String addCourseGet(Model model) {
        System.out.println("=============================================================== just entered /addcourse GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% created new course, nothing is attached to it, about to add it to model");

        //        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% created new skill, attached currPerson to it, about to add it to model");
//        Skill skill = new Skill();
//        skill.setMyPerson(p);
//        model.addAttribute("newSkill", skill);


//        // testing
//        // create a new course with NO person attached to it
//        // result: course saved in repo ok, join table all nulls
//        Course c = new Course();
//        c.setCredits(1.5f);
//        c.setInstructor("Instructor Bob");
//        c.setTitle("Bob's course");
//        courseRepo.save(c);
//
//        // create another course
//        Course c2 = new Course();
//        c2.setCredits(2.5f);
//        c2.setInstructor("Instructor Two");
//        c2.setTitle("Course Title Two");
//        courseRepo.save(c2);
//
//        // create another course
//        Course c3 = new Course();
//        c3.setCredits(3.5f);
//        c3.setInstructor("Instructor Three");
//        c3.setTitle("Course Title Three");
//        courseRepo.save(c3);
//
//        // create another course
//        Course c4 = new Course();
//        c4.setCredits(4.5f);
//        c4.setInstructor("Instructor Four");
//        c4.setTitle("Course Title Four");
//        courseRepo.save(c4);
//
//
//        // create a new person with NO course attached to it
//        // result: exactly same as above but for person
//        Person p = new Person();
//        p.setEmail("a@b.com");
//        p.setNameFirst("Nate");
//        p.setNameLast("Merris");
//        personRepo.save(p);
//
//        Person p2 = new Person();
//        p2.setEmail("x@y.com");
//        p2.setNameLast("LastNameTwo");
//        p2.setNameFirst("FirstNameTwo");
//        personRepo.save(p2);
//
//
//        // now attach a set of courses to the person
//        HashSet<Course> myCourses = new HashSet<>();
//        myCourses.add(c);
//        myCourses.add(c2);
//        myCourses.add(c3);
//        p.addCourses(myCourses);
//        personRepo.save(p);
//
//        // check to make sure it worked
//        System.out.println("***************** just added a set of courses to person id: " + p.getId());
//        System.out.println("***************** courseRepo.findAllByMyPeopleIs(p): ");
//        for (Course anyCourse : courseRepo.findAllByPeopleIs(p)) {
//            System.out.println("********* course id: " + anyCourse.getId() + ", title: " + anyCourse.getTitle());
//        }
//
//        // remove a set of courses from the person
//        HashSet<Course> toRemove = new HashSet<>();
//        toRemove.add(c);
//        toRemove.add(c2);
//        toRemove.add(c4); // not actually attached to p, testing for check box input... !!!!!!!!!! works!!!!!!!!!!!!
////        p.removeCourse(c);
//        p.removeCourses(toRemove);
//
//        // now add another set, this simulates a user checking/unchecking numerous course boxes in student reg page
//        HashSet<Course> toAddBack = new HashSet<>();
//        toAddBack.add(c2);
//        toAddBack.add(c4);
//        p.addCourses(toAddBack);
//
//        personRepo.save(p);
//
//        // check to make sure it worked
//        System.out.println("***************** just added, then removed, then added again courses");
////        System.out.println("***************** just removed course id: " + c.getId());
//        System.out.println("***************** then saved to personRepo... courseRepo.findAllByMyPeopleIs(p): ");
//        for (Course anyCourse : courseRepo.findAllByPeopleIs(p)) {
//            System.out.println("********* course id: " + anyCourse.getId() + ", title: " + anyCourse.getTitle());
//        }
//
//
//        // try to remove a person from a course... WORKS!!! but must remove the course from EACH PERSON before removing
//        // the persons from the course, because database stuff
//        HashSet<Person> personsToRemove = new HashSet<>();
//        personsToRemove.add(p); // only removing one person, could do many
//        for (Person somePersonsToRemoveFromOneCourse : personsToRemove) {
//            somePersonsToRemoveFromOneCourse.removeCourse(c4);
//        }
//
//        // NOW that the course in question has been removed from EVERY person you are trying to remove from the course
//        // you can now remove the persons from the course and finally save it to repo
//        c4.removePersons(personsToRemove);
//        courseRepo.save(c4);
//
//
//        // get all the persons who are enrolled in a given course
//        System.out.println("******************************** persons enrolled in courseId: " + c3.getId() + ", title: " + c3.getTitle());
//        for (Person enrolledPerson : personRepo.findAllByCoursesIs(c3)) {
//            System.out.println("*********** personId: " + enrolledPerson.getId() + ", first name: " + enrolledPerson.getNameFirst());
//        }

        Course course = new Course();
        model.addAttribute("newCourse", course);

        // see boolean flag to highlight the appropriate navbar link
        model.addAttribute("highlightDirectory", false);
        model.addAttribute("highlightCourses", false);
        model.addAttribute("highlightAddCourse", true);
        model.addAttribute("highlightAddStudent", false);

        return "addcourse";
    }


    @PostMapping("/addcourse")
    public String addCoursePost(@Valid @ModelAttribute("newCourse") Course course,
                               BindingResult bindingResult, Model model) {
        System.out.println("=============================================================== just entered /addcourse POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());


        if(bindingResult.hasErrors()) {
            model.addAttribute("highlightDirectory", false);
            model.addAttribute("highlightCourses", false);
            model.addAttribute("highlightAddCourse", true);
            model.addAttribute("highlightAddStudent", false);
            return "addcourse";
        }

        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% about to save course to Repo");
        courseRepo.save(course);

        // go to course listing page if successfully added a course, no need for confirmation page here
        return "redirect:/courselist";
    }
    

    @GetMapping("/summary/{id}")
    public String summary(@PathVariable("id") long id, Model model) {
        System.out.println("=============================================================== just entered /summary/{id} GET");

        // set the current person id to the incoming path variable, which is the id of student that was just clicked
        currPerson.setPersonId(id);
        System.out.println("=========================================== just set currPerson.getPersonId(): " + currPerson.getPersonId());

        Person p = personRepo.findOne(id);
        model.addAttribute("numEds", educationRepo.countAllByMyPersonIs(p));
        model.addAttribute("numWorkExps", workExperienceRepo.countAllByMyPersonIs(p));
        model.addAttribute("numSkills", skillRepo.countAllByMyPersonIs(p));
        addPersonNameToModel(model);

        model.addAttribute("courses", courseRepo.findAllByPeopleIs(p));
        String s = String.format("Student: %s %s - ID: %d", p.getNameFirst(), p.getNameLast(), p.getId());
        model.addAttribute("summaryBarTitle", s);

        // add the total number of credits for this student to the model
        long sumCredits = 0;
        for (Course c : courseRepo.findAllByPeopleIs(p)) {
            sumCredits += c.getCredits();
        }
        model.addAttribute("sumCredits", sumCredits);

        model.addAttribute("highlightDirectory", true);
        model.addAttribute("highlightCourses", false);
        model.addAttribute("highlightAddCourse", false);
        model.addAttribute("highlightAddStudent", false);

        return "summary";
    }


    @GetMapping("/studentregistration")
    public String studentRegistrationGet(Model model) {
        System.out.println("=============================================================== just entered /studentregistration GET");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());

        // get the current person
        Person p = personRepo.findOne(currPerson.getPersonId());

        // create a new HashSet to hold the courses that the current student is NOT currently registered in
        Set<Course> remainingCourses = new HashSet<>();
        // fill it up with ALL courses initially
        for (Course c : courseRepo.findAll()) {
            remainingCourses.add(c);
        }
        // now remove each course that the student is currently registered in
        for (Course c : courseRepo.findAll()) {
            for(Course enrolledCourse : p.getCourses()) {
                if(c.getId() == enrolledCourse.getId()) {
                    remainingCourses.remove(enrolledCourse);
                }
            }
        }
        // and add the set of remaining courses to the model, these will NOT be pre-checked
        model.addAttribute("allRemainingCourses", remainingCourses);

        // add the courses that the current student is currently registered in, these will all be pre-checked
        model.addAttribute("currentlyRegisteredCourses", p.getCourses());

        // add student name and ID to the model
        String s = String.format("Student: %s %s - ID: %d", p.getNameFirst(), p.getNameLast(), p.getId());
        model.addAttribute("summaryBarTitle", s);

        model.addAttribute("highlightDirectory", true);
        model.addAttribute("highlightCourses", false);
        model.addAttribute("highlightAddCourse", false);
        model.addAttribute("highlightAddStudent", false);

        // sum up all the credits for courses the student is currently registered in
        long sumCredits = 0;
        for (Course c : p.getCourses()) {
            sumCredits += c.getCredits();
        }
        model.addAttribute("sumCredits", sumCredits);

        return "studentregistration";
    }


    @PostMapping("/studentregistration")
    public String studentRegistrationPost(@RequestParam(value = "checkedIds", required = false) long[] checkedCourseIds) {
        System.out.println("=============================================================== just entered /studentregistration POST");
        System.out.println("=========================================== currPerson.getPersonId(): " + currPerson.getPersonId());


        // testing
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! just got these ids from check boxes (these were the checked boxes)...");
        try {
            for (long id : checkedCourseIds) {
                System.out.println(id + "  <-----");
            }
        } catch (Exception e) {
            System.out.println("THERE WERE NO COURSES CHECKED, SO checkedCourseIds was NULL, that's just fine");
        }

        //get the current Person
        Person p = personRepo.findOne(currPerson.getPersonId());

        // wipe out all the courses person is currently registered in
        p.removeAllCourses();

        // build a set of courses based on the ids we just got back from the checkbox form, may be zero
        // need to catch a null pointer exception if checkedCourseIds is null (user unchecked ALL courses)
        try {
            for(long id : checkedCourseIds) {
                p.addCourse(courseRepo.findOne(id));
            }
        }
        catch (Exception e)
        {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! NO COURSES WERE ADDED, SO STUDENT SHOULD HAVE ZERO COURSES NOW");
            // nothing to do here, it's okay if no courses were added to person
        }

        // save the person
        personRepo.save(p);

        // show the registration page, it's as good as a confirmation page, because it shows all the courses the student
        // is registered in, and will reflect any changes just made
        return "redirect:/studentregistration";
    }


    @GetMapping("/courseregistration/{id}")
    public String courseRegistrationGet(@PathVariable("id") long id, Model model) {
        System.out.println("=============================================================== just entered /courseregistration GET");
        System.out.println("=========================================== courseId: " + id);

        // get the current being viewed
        Course c = courseRepo.findOne(id);

        // add all the students that are registered to this course to the model, all will be checked initially
        model.addAttribute("registeredStudents", c.getPeople());

        // add summary course info to model
        String s = String.format("Course: %s - Instructor: %s", c.getTitle(), c.getInstructor());
        model.addAttribute("summaryBarTitle", s);

        // add the course ID to the model so that the POST method knows what course we are dealing with
        model.addAttribute("courseId", id);

        model.addAttribute("highlightDirectory", false);
        model.addAttribute("highlightCourses", true);
        model.addAttribute("highlightAddCourse", false);
        model.addAttribute("highlightAddStudent", false);

        // disable the submit button if there are no students currently registered for this course
        model.addAttribute("disableSubmit", c.getNumRegistered() == 0);

        return "courseregistration";
    }


    // incoming RequestParam needs to be full object type Long in order to convert it to a Set
    @PostMapping("/courseregistration/{id}")
    public String courseRegistrationPost(@PathVariable("id") long id,
                                         @RequestParam(value = "checkedIds", required = false) Long[] checkedStudentIds) {
        System.out.println("=============================================================== just entered /courseregistration POST");
        System.out.println("=========================================== courseId: " + id);

        // get the course
        Course c = courseRepo.findOne(id);


        // testing
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! just got these ids from check boxes (these were the checked boxes)...");
        try {
            for (long checkedId : checkedStudentIds) {
                System.out.println(checkedId + "  <-----");
            }
        } catch (Exception e) {
            System.out.println("THERE WERE NO STUDENTS CHECKED, SO checkedStudentIds was NULL, that's just fine");
        }


        // build a set of ids to unregister from this course, start with a set of ALL the ids currently registered
        Set<Long> studentIdsToUnregister = new HashSet<>();
        for (Person p : c.getPeople()) {
            studentIdsToUnregister.add(p.getId());
        }


        try {
            // convert the incoming array to a Set of Longs, these are the student ids to keep registered in this course
            // this may be null if every student was unchecked, which will throw an exception
            Set<Long> checkedStudentIdsSet = new HashSet<>(Arrays.asList(checkedStudentIds));

            // remove the students that remained checked from the original set of all students that were checked
            // this is essentially the difference of the initial set and the final set after user possibly modified it
            studentIdsToUnregister.removeAll(checkedStudentIdsSet);
        } catch (Exception e) {
            // checkedStudentIds must have been null, ie ALL students were unchecked from this course, which means we
            // want to remove all students from this course, so leave studentsToUnRegister alone, becuase it already
            // contains a set of all the currently registered students
        }


        // remove this course from each student that was unchecked, then remove same person from this course
        for (long idToUnregister : studentIdsToUnregister) {
            personRepo.findOne(idToUnregister).removeCourse(c);
            c.removePerson(personRepo.findOne(idToUnregister));
        }

        // now save this course
        courseRepo.save(c);

        // show the registration page as a confirmation
        // TODO make some kind of toast or message to indicate confirmation
        return "redirect:/courseregistration/" + id;
    }



        /**
         * The navbar links are disabled depending on the number of records in the various db tables.  For example, we
         * do not want to allow the user to click the EditDetails link if there are no records in any db table.
         * Note: the 'highlighted' nav bar link is set individually in each route.  Also, the navbar links contain badges
         * that show the current counts for various db tables.  These counts are updated here and will always reflect the
         * current state of the db tables.
         * @return an updated NavBarState, but the highlighted navbar link must still be set individually
         */
    private NavBarState getPageLinkState() {
        NavBarState state = new NavBarState();

        // get the current Person, this will return null if currPerson has not been set yet, which is ok
        // this will happen in /addperson GET when a new person is being entered
        Person p = personRepo.findOne(currPerson.getPersonId());

        if(p != null) {
            // if this line is reached, then there must be a Person already entered

            // add the current table counts, so the navbar badges know what to display
            state.setNumSkills(skillRepo.countAllByMyPersonIs(p));
            state.setNumWorkExps(workExperienceRepo.countAllByMyPersonIs(p));
            state.setNumEdAchievements(educationRepo.countAllByMyPersonIs(p));

            // disable links as necessary... don't allow them to click any links if the repos contain too many records
            state.setDisableAddEdLink(educationRepo.countAllByMyPersonIs(p) >= 10);
            state.setDisableAddSkillLink(skillRepo.countAllByMyPersonIs(p) >= 20);
            state.setDisableAddWorkExpLink(workExperienceRepo.countAllByMyPersonIs(p) >= 10);

            // enable the edit details link... the user has already entered a Person, so it's ok to to allow them to edit
            state.setDisableEditDetailsLink(false);

            // disable show final resume link until at least one ed achievement, skill, and personal info has been entered
            state.setDisableShowFinalLink(skillRepo.countAllByMyPersonIs(p) == 0 || educationRepo.countAllByMyPersonIs(p) == 0);
        }
        else {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! p == null in getPageLinkState, so must not have found a Person in personRepo");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!! initializing navbar state with appropriate values....");

            // zero out the counts for the badges, because user has not entered a Person yet
            state.setNumSkills(0);
            state.setNumWorkExps(0);
            state.setNumEdAchievements(0);

            // disable links... user has not entered a Person yet
            state.setDisableAddEdLink(true);
            state.setDisableAddSkillLink(true);
            state.setDisableAddWorkExpLink(true);

            // disable edit link... user has not entered a Person yet
            state.setDisableEditDetailsLink(true);

            state.setDisableShowFinalLink(true);
        }

        return state;
    }
    
    
    /**
     * Adds the entire contents of each database table to model. Note that the object names used here must match
     * the names in the template being used: 'persons', 'edAchievements', 'workExperiences', 'skills'
     *
     * @return model, now with the entire contents of each repo
     */
//    private void addDbContentsToModel(Model model) {
//        // there is only one person
//        model.addAttribute("persons", personRepo.findAll());
//        model.addAttribute("edAchievements", educationRepo.findAll());
//        model.addAttribute("workExperiences", workExperienceRepo.findAll());
//        model.addAttribute("skills", skillRepo.findAll());
//    }


    /**
     * Composes a person using the data from the tables in the database.  All records are read out and lists are
     * populated in person for educational achievements, work experiences, and skills.  The person itself should already
     * contain a first and last name, and an email address.  After calling this function, person should contain
     * sufficient info to create a resume.
     * @param person the Person to compose
     */
//    private void composePerson(Person person) {
//        // get all the records from the db
//        ArrayList<EducationAchievement> edsArrayList = new ArrayList<>();
//        for(EducationAchievement item : educationRepo.findAll()) {
//            edsArrayList.add(item);
//        }
//        // add it to our Person
////        person.setEducationAchievements(edsArrayList);
//
//        ArrayList<WorkExperience> weArrayList = new ArrayList<>();
//        for(WorkExperience item : workExperienceRepo.findAll()) {
//            weArrayList.add(item);
//        }
////        person.setWorkExperiences(weArrayList);
//
//        ArrayList<Skill> skillsArrayList = new ArrayList<>();
//        for(Skill item : skillRepo.findAll()) {
//            skillsArrayList.add(item);
//        }
////        person.setSkills(skillsArrayList);
//    }


    /**
     * Adds an object (firstAndLastName) to model, that is a String of the first and last name of the Person for this
     * resume. If the Person table is empty, an appropriate message is added to the model that will indicate to the user
     * that they need to add start the resume by adding personal details.  This 'backup' String should never be seen..
     * unless the user manually types in, for example, /addskill in their browser BEFORE they have entered personal details.
     * NOTE: each template that uses this must refer to it as 'firstAndLastName'
     *
     * @return model, now with 'firstAndLastName' attribute already added and ready to use in a template
     */
    private void addPersonNameToModel(Model model) {
        try {
            // try to get the single Person from the db
//            Person p = personRepo.findAll().iterator().next();
            Person p = personRepo.findOne(currPerson.getPersonId());
            // if there was a Person, add their full name to the model
            model.addAttribute("firstAndLastName", p.getNameFirst() + " " + p.getNameLast());
        } catch (Exception e) {
            // must not have found a Person in the db, so use a placeholder name
            // this is really convenient for testing, but it also makes the app less likely to crash
            // the only way this will be shown is if the user manually enters a route before completing the
            // personal details section, because the other resume section links are disabled until the user
            // has entered their personal info
            model.addAttribute("firstAndLastName", "Please start by entering personal details");
        }
    }


}
