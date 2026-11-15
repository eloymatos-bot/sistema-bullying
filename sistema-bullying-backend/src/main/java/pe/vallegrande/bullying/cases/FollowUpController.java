package pe.vallegrande.bullying.cases;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/cases/{id}/followups")
public class FollowUpController {
 private final FollowUpService service;
 public FollowUpController(FollowUpService service){this.service=service;}
 @GetMapping public FollowUpService.Timeline get(@PathVariable long id,Principal p){return service.timeline(id,p.getName());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public FollowUpService.Timeline add(@PathVariable long id,@RequestBody FollowUpService.Input input,Principal p){return service.add(id,input,p.getName());}
}
