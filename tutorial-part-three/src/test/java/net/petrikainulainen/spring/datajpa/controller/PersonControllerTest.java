package net.petrikainulainen.spring.datajpa.controller;

import net.petrikainulainen.spring.datajpa.dto.PersonDTO;
import net.petrikainulainen.spring.datajpa.dto.SearchDTO;
import net.petrikainulainen.spring.datajpa.dto.SearchType;
import net.petrikainulainen.spring.datajpa.model.Person;
import net.petrikainulainen.spring.datajpa.model.PersonTestUtil;
import net.petrikainulainen.spring.datajpa.service.PersonNotFoundException;
import net.petrikainulainen.spring.datajpa.service.PersonService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.*;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Petri Kainulainen
 */
public class PersonControllerTest extends AbstractTestController {

    private static final String FIELD_NAME_FIRST_NAME = "firstName";
    private static final String FIELD_NAME_LAST_NAME = "lastName";
    
    private static final Long PERSON_ID = Long.valueOf(5);
    private static final String FIRST_NAME = "Foo";
    private static final String FIRST_NAME_UPDATED = "FooUpdated";
    private static final String LAST_NAME = "Bar";
    private static final String LAST_NAME_UPDATED = "BarUpdated";

    private PersonController controller;
    
    private PersonService personServiceMock;

    @Override
    public void setUpTest() {
        controller = new PersonController();

        controller.setMessageSource(getMessageSourceMock());

        personServiceMock = mock(PersonService.class);
        controller.setPersonService(personServiceMock);
    }
    
    @Test
    public void delete() throws PersonNotFoundException {
        Person deleted = PersonTestUtil.createModelObject(PERSON_ID, FIRST_NAME, LAST_NAME);
        when(personServiceMock.delete(PERSON_ID)).thenReturn(deleted);
        
        initMessageSourceForFeedbackMessage(PersonController.FEEDBACK_MESSAGE_KEY_PERSON_DELETED);
        
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        String view = controller.delete(PERSON_ID, attributes);
        
        verify(personServiceMock, times(1)).delete(PERSON_ID);
        verifyNoMoreInteractions(personServiceMock);
        assertFeedbackMessage(attributes, PersonController.FEEDBACK_MESSAGE_KEY_PERSON_DELETED);
        
        String expectedView = createExpectedRedirectViewPath(PersonController.REQUEST_MAPPING_LIST);
        assertEquals(expectedView, view);
    }
    
    @Test
    public void deleteWhenPersonIsNotFound() throws PersonNotFoundException {
        when(personServiceMock.delete(PERSON_ID)).thenThrow(new PersonNotFoundException());
        
        initMessageSourceForErrorMessage(PersonController.ERROR_MESSAGE_KEY_DELETED_PERSON_WAS_NOT_FOUND);
        
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        String view = controller.delete(PERSON_ID, attributes);
        
        verify(personServiceMock, times(1)).delete(PERSON_ID);
        verifyNoMoreInteractions(personServiceMock);
        assertErrorMessage(attributes, PersonController.ERROR_MESSAGE_KEY_DELETED_PERSON_WAS_NOT_FOUND);
        
        String expectedView = createExpectedRedirectViewPath(PersonController.REQUEST_MAPPING_LIST);
        assertEquals(expectedView, view);
    }
    
    @Test
    public void search() {
        SearchDTO searchCriteria = createSearchCriteria(LAST_NAME, SearchType.METHOD_NAME);
        List<Person> expected = new ArrayList<Person>();
        when(personServiceMock.search(searchCriteria)).thenReturn(expected);
        
        BindingAwareModelMap model = new BindingAwareModelMap();
        String view = controller.search(searchCriteria, model);
        
        verify(personServiceMock, times(1)).search(searchCriteria);
        verifyNoMoreInteractions(personServiceMock);
        
        assertEquals(PersonController.PERSON_SEARCH_RESULT_VIEW, view);
        List<Person> actual = (List<Person>) model.asMap().get(PersonController.MODEL_ATTRIBUTE_PERSONS);
        assertEquals(expected, actual);
    }
    
    private SearchDTO createSearchCriteria(String searchTerm, SearchType searchType) {
        SearchDTO searchCriteria = new SearchDTO();
        
        searchCriteria.setSearchTerm(searchTerm);
        searchCriteria.setSearchType(searchType);
        
        return searchCriteria;
    }
    
    @Test
    public void showCreatePersonForm() {
        Model model = new BindingAwareModelMap();
        
        String view = controller.showCreatePersonForm(model);
        
        verifyZeroInteractions(personServiceMock);
        
        assertEquals(PersonController.PERSON_ADD_FORM_VIEW, view);

        PersonDTO added = (PersonDTO) model.asMap().get(PersonController.MODEL_ATTIRUTE_PERSON);
        assertNotNull(added);
        
        assertNull(added.getId());
        assertNull(added.getFirstName());
        assertNull(added.getLastName());
    }

    @Test
    public void submitCreatePersonForm() {        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/create", "POST");
        
        PersonDTO created = PersonTestUtil.createDTO(PERSON_ID, FIRST_NAME, LAST_NAME);
        Person model = PersonTestUtil.createModelObject(PERSON_ID, FIRST_NAME, LAST_NAME);
        when(personServiceMock.create(created)).thenReturn(model);

        initMessageSourceForFeedbackMessage(PersonController.FEEDBACK_MESSAGE_KEY_PERSON_CREATED);
        
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        BindingResult result = bindAndValidate(mockRequest, created);
        
        String view = controller.submitCreatePersonForm(created, result, attributes);
        
        verify(personServiceMock, times(1)).create(created);
        verifyNoMoreInteractions(personServiceMock);
        
        String expectedViewPath = createExpectedRedirectViewPath(PersonController.REQUEST_MAPPING_LIST);
        assertEquals(expectedViewPath, view);
        
        assertFeedbackMessage(attributes, PersonController.FEEDBACK_MESSAGE_KEY_PERSON_CREATED);
        
        verify(personServiceMock, times(1)).create(created);
        verifyNoMoreInteractions(personServiceMock);
    }
    
    @Test
    public void submitEmptyCreatePersonForm() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/create", "POST");
        
        PersonDTO created = new PersonDTO();
        
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        BindingResult result = bindAndValidate(mockRequest, created);
        
        String view = controller.submitCreatePersonForm(created, result, attributes);
        
        verifyZeroInteractions(personServiceMock);
        
        assertEquals(PersonController.PERSON_ADD_FORM_VIEW, view);
        assertFieldErrors(result, FIELD_NAME_FIRST_NAME, FIELD_NAME_LAST_NAME);
    }

    @Test
    public void submitCreatePersonFormWithEmptyFirstName() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/create", "POST");

        PersonDTO created = PersonTestUtil.createDTO(null, null, LAST_NAME);

        RedirectAttributes attributes = new RedirectAttributesModelMap();
        BindingResult result = bindAndValidate(mockRequest, created);

        String view = controller.submitCreatePersonForm(created, result, attributes);

        verifyZeroInteractions(personServiceMock);

        assertEquals(PersonController.PERSON_ADD_FORM_VIEW, view);
        assertFieldErrors(result, FIELD_NAME_FIRST_NAME);
    }

    @Test
    public void submitCreatePersonFormWithEmptyLastName() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/create", "POST");

        PersonDTO created = PersonTestUtil.createDTO(null, FIRST_NAME, null);

        RedirectAttributes attributes = new RedirectAttributesModelMap();
        BindingResult result = bindAndValidate(mockRequest, created);

        String view = controller.submitCreatePersonForm(created, result, attributes);

        verifyZeroInteractions(personServiceMock);

        assertEquals(PersonController.PERSON_ADD_FORM_VIEW, view);
        assertFieldErrors(result, FIELD_NAME_LAST_NAME);
    }
    
    @Test
    public void showEditPersonForm() {
        Person person = PersonTestUtil.createModelObject(PERSON_ID, FIRST_NAME, LAST_NAME);
        when(personServiceMock.findById(PERSON_ID)).thenReturn(person);
        
        Model model = new BindingAwareModelMap();
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        
        String view = controller.showEditPersonForm(PERSON_ID, model, attributes);
        
        verify(personServiceMock, times(1)).findById(PERSON_ID);
        verifyNoMoreInteractions(personServiceMock);
        
        assertEquals(PersonController.PERSON_EDIT_FORM_VIEW, view);
        
        PersonDTO formObject = (PersonDTO) model.asMap().get(PersonController.MODEL_ATTIRUTE_PERSON);

        assertNotNull(formObject);
        assertEquals(person.getId(), formObject.getId());
        assertEquals(person.getFirstName(), formObject.getFirstName());
        assertEquals(person.getLastName(), formObject.getLastName());
    }
    
    @Test
    public void showEditPersonFormWhenPersonIsNotFound() {
        when(personServiceMock.findById(PERSON_ID)).thenReturn(null);
        
        initMessageSourceForErrorMessage(PersonController.ERROR_MESSAGE_KEY_EDITED_PERSON_WAS_NOT_FOUND);
        
        Model model = new BindingAwareModelMap();
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        
        String view = controller.showEditPersonForm(PERSON_ID, model, attributes);
        
        verify(personServiceMock, times(1)).findById(PERSON_ID);
        verifyNoMoreInteractions(personServiceMock);
        
        String expectedView = createExpectedRedirectViewPath(PersonController.REQUEST_MAPPING_LIST);
        assertEquals(expectedView, view);

        assertErrorMessage(attributes, PersonController.ERROR_MESSAGE_KEY_EDITED_PERSON_WAS_NOT_FOUND);
    }
    
    @Test
    public void submitEditPersonForm() throws PersonNotFoundException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/edit", "POST");
        PersonDTO updated = PersonTestUtil.createDTO(PERSON_ID, FIRST_NAME_UPDATED, LAST_NAME_UPDATED);
        Person person = PersonTestUtil.createModelObject(PERSON_ID, FIRST_NAME_UPDATED, LAST_NAME_UPDATED);
        
        when(personServiceMock.update(updated)).thenReturn(person);
        
        initMessageSourceForFeedbackMessage(PersonController.FEEDBACK_MESSAGE_KEY_PERSON_EDITED);
        
        BindingResult bindingResult = bindAndValidate(mockRequest, updated);
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        
        String view = controller.submitEditPersonForm(updated, bindingResult, attributes);
        
        verify(personServiceMock, times(1)).update(updated);
        verifyNoMoreInteractions(personServiceMock);
        
        String expectedView = createExpectedRedirectViewPath(PersonController.REQUEST_MAPPING_LIST);
        assertEquals(expectedView, view);

        assertFeedbackMessage(attributes, PersonController.FEEDBACK_MESSAGE_KEY_PERSON_EDITED);
        
        assertEquals(updated.getFirstName(), person.getFirstName());
        assertEquals(updated.getLastName(), person.getLastName());
    }
    
    @Test
    public void submitEditPersonFormWhenPersonIsNotFound() throws PersonNotFoundException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/edit", "POST");
        PersonDTO updated = PersonTestUtil.createDTO(PERSON_ID, FIRST_NAME_UPDATED, LAST_NAME_UPDATED);
        
        when(personServiceMock.update(updated)).thenThrow(new PersonNotFoundException());
        initMessageSourceForErrorMessage(PersonController.ERROR_MESSAGE_KEY_EDITED_PERSON_WAS_NOT_FOUND);
        
        BindingResult bindingResult = bindAndValidate(mockRequest, updated);
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        
        String view = controller.submitEditPersonForm(updated, bindingResult, attributes);
        
        verify(personServiceMock, times(1)).update(updated);
        verifyNoMoreInteractions(personServiceMock);
        
        String expectedView = createExpectedRedirectViewPath(PersonController.REQUEST_MAPPING_LIST);
        assertEquals(expectedView, view);

        assertErrorMessage(attributes, PersonController.ERROR_MESSAGE_KEY_EDITED_PERSON_WAS_NOT_FOUND);
    }
    
    @Test
    public void submitEmptyEditPersonForm() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/edit", "POST");
        PersonDTO updated = PersonTestUtil.createDTO(PERSON_ID, null, null);
        
        BindingResult bindingResult = bindAndValidate(mockRequest, updated);
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        
        String view = controller.submitEditPersonForm(updated, bindingResult, attributes);
        
        verifyZeroInteractions(personServiceMock);
        
        assertEquals(PersonController.PERSON_EDIT_FORM_VIEW, view);
        assertFieldErrors(bindingResult, FIELD_NAME_FIRST_NAME, FIELD_NAME_LAST_NAME);
    }

    @Test
    public void submitEditPersonFormWhenFirstNameIsEmpty() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/edit", "POST");
        PersonDTO updated = PersonTestUtil.createDTO(PERSON_ID, null, LAST_NAME_UPDATED);

        BindingResult bindingResult = bindAndValidate(mockRequest, updated);
        RedirectAttributes attributes = new RedirectAttributesModelMap();

        String view = controller.submitEditPersonForm(updated, bindingResult, attributes);

        verifyZeroInteractions(personServiceMock);

        assertEquals(PersonController.PERSON_EDIT_FORM_VIEW, view);
        assertFieldErrors(bindingResult, FIELD_NAME_FIRST_NAME);
    }

    @Test
    public void submitEditPersonFormWhenLastNameIsEmpty() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("/person/edit", "POST");
        PersonDTO updated = PersonTestUtil.createDTO(PERSON_ID, FIRST_NAME_UPDATED, null);

        BindingResult bindingResult = bindAndValidate(mockRequest, updated);
        RedirectAttributes attributes = new RedirectAttributesModelMap();

        String view = controller.submitEditPersonForm(updated, bindingResult, attributes);

        verifyZeroInteractions(personServiceMock);

        assertEquals(PersonController.PERSON_EDIT_FORM_VIEW, view);
        assertFieldErrors(bindingResult, FIELD_NAME_LAST_NAME);
    }
    
    @Test
    public void showList() {
        List<Person> persons = new ArrayList<Person>();
        when(personServiceMock.findAll()).thenReturn(persons);
        
        Model model = new BindingAwareModelMap();
        String view = controller.showList(model);
        
        verify(personServiceMock, times(1)).findAll();
        verifyNoMoreInteractions(personServiceMock);
        
        assertEquals(PersonController.PERSON_LIST_VIEW, view);
        assertEquals(persons, model.asMap().get(PersonController.MODEL_ATTRIBUTE_PERSONS));

        SearchDTO searchCriteria = (SearchDTO) model.asMap().get(PersonController.MODEL_ATTRIBUTE_SEARCHCRITERIA);
        assertNotNull(searchCriteria);
        assertNull(searchCriteria.getSearchTerm());
        assertNull(searchCriteria.getSearchType());
    }
}
